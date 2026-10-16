#!/usr/bin/env bash
# Provisions the backend on AWS: ECR image + one EC2 t3.micro running the container.
# Usage: AWS_PROFILE=hotel ./deploy/aws/provision.sh
# Safe to re-run: every resource is created only if it does not exist yet.
set -euo pipefail
export MSYS_NO_PATHCONV=1 # keeps Git Bash on Windows from rewriting /paths in arguments

REGION="${AWS_REGION:-us-east-1}"
APP="hotel-reservations"
ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
IMAGE="$REGISTRY/$APP:latest"
ROLE="hotel-backend-ec2-role"
SG_NAME="hotel-backend-sg"
JWT_PARAM="/hotel/jwt-secret"
CORS_PARAM="/hotel/cors-allowed-origins"
export AWS_DEFAULT_REGION="$REGION"

echo "Account $ACCOUNT, region $REGION"

# 1. Container image ---------------------------------------------------------
aws ecr describe-repositories --repository-names "$APP" >/dev/null 2>&1 \
  || aws ecr create-repository --repository-name "$APP" --image-scanning-configuration scanOnPush=true >/dev/null
aws ecr get-login-password | docker login --username AWS --password-stdin "$REGISTRY"
docker build --platform linux/amd64 -t "$IMAGE" .
docker push "$IMAGE"

# 2. Configuration in SSM Parameter Store (secret never touches the repo) ----
if ! aws ssm get-parameter --name "$JWT_PARAM" >/dev/null 2>&1; then
  aws ssm put-parameter --name "$JWT_PARAM" --type SecureString \
    --value "$(openssl rand -base64 48 | tr -d '\n')" >/dev/null
fi
aws ssm get-parameter --name "$CORS_PARAM" >/dev/null 2>&1 \
  || aws ssm put-parameter --name "$CORS_PARAM" --type String --value "http://localhost:5173" >/dev/null

# 3. IAM role for the instance (pull from ECR, read parameters, SSM agent) ----
if ! aws iam get-role --role-name "$ROLE" >/dev/null 2>&1; then
  aws iam create-role --role-name "$ROLE" --assume-role-policy-document \
    '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}' >/dev/null
  aws iam attach-role-policy --role-name "$ROLE" --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
  aws iam attach-role-policy --role-name "$ROLE" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
  aws iam put-role-policy --role-name "$ROLE" --policy-name read-hotel-parameters --policy-document \
    "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":\"ssm:GetParameter\",\"Resource\":\"arn:aws:ssm:$REGION:$ACCOUNT:parameter/hotel/*\"}]}"
  aws iam create-instance-profile --instance-profile-name "$ROLE" >/dev/null
  aws iam add-role-to-instance-profile --instance-profile-name "$ROLE" --role-name "$ROLE"
  echo "Waiting for the instance profile to propagate..."
  sleep 15
fi

# 4. Security group: port 8080 reachable only from CloudFront -----------------
VPC_ID="$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)"
SG_ID="$(aws ec2 describe-security-groups --filters Name=group-name,Values="$SG_NAME" Name=vpc-id,Values="$VPC_ID" \
  --query 'SecurityGroups[0].GroupId' --output text)"
if [ "$SG_ID" = "None" ]; then
  SG_ID="$(aws ec2 create-security-group --group-name "$SG_NAME" --vpc-id "$VPC_ID" \
    --description "Hotel backend, CloudFront only" --query GroupId --output text)"
  CF_PREFIX_LIST="$(aws ec2 describe-managed-prefix-lists \
    --filters Name=prefix-list-name,Values=com.amazonaws.global.cloudfront.origin-facing \
    --query 'PrefixLists[0].PrefixListId' --output text)"
  aws ec2 authorize-security-group-ingress --group-id "$SG_ID" \
    --ip-permissions "IpProtocol=tcp,FromPort=8080,ToPort=8080,PrefixListIds=[{PrefixListId=$CF_PREFIX_LIST}]" >/dev/null
fi

# 5. Instance ------------------------------------------------------------------
INSTANCE_ID="$(aws ec2 describe-instances \
  --filters Name=tag:Name,Values="$APP" Name=instance-state-name,Values=pending,running,stopped \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)"
if [ "$INSTANCE_ID" = "None" ]; then
  AMI="$(aws ssm get-parameter --name /aws/service/ami-amazon-linux-latest/al2023-ami-kernel-default-x86_64 \
    --query Parameter.Value --output text)"
  USER_DATA="$(cat <<EOF
#!/bin/bash
set -euo pipefail
dnf install -y docker
systemctl enable --now docker

cat > /usr/local/bin/hotel-start <<'SCRIPT'
#!/bin/bash
# Pulls the latest image and (re)starts the container. Run again to redeploy.
set -euo pipefail
REGION=$REGION
JWT_SECRET=\$(aws ssm get-parameter --region \$REGION --name $JWT_PARAM --with-decryption --query Parameter.Value --output text)
CORS=\$(aws ssm get-parameter --region \$REGION --name $CORS_PARAM --query Parameter.Value --output text)
aws ecr get-login-password --region \$REGION | docker login --username AWS --password-stdin $REGISTRY
docker pull $IMAGE
docker rm -f hotel 2>/dev/null || true
docker run -d --name hotel --restart always -p 8080:8080 \\
  -e JWT_SECRET="\$JWT_SECRET" -e CORS_ALLOWED_ORIGINS="\$CORS" \\
  -e JAVA_TOOL_OPTIONS="-Xmx400m" $IMAGE
SCRIPT
chmod +x /usr/local/bin/hotel-start
/usr/local/bin/hotel-start
EOF
)"
  INSTANCE_ID="$(aws ec2 run-instances --image-id "$AMI" --instance-type t3.micro \
    --iam-instance-profile Name="$ROLE" --security-group-ids "$SG_ID" \
    --metadata-options HttpTokens=required,HttpEndpoint=enabled \
    --block-device-mappings 'DeviceName=/dev/xvda,Ebs={VolumeSize=12,VolumeType=gp3,Encrypted=true}' \
    --user-data "$USER_DATA" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$APP}]" \
    --query 'Instances[0].InstanceId' --output text)"
fi
aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"
DNS="$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PublicDnsName' --output text)"

echo
echo "Instance:   $INSTANCE_ID"
echo "Origin DNS: $DNS   (use it as the API origin in CloudFront, port 8080)"
echo "The container needs about 2-3 minutes after boot before it answers."
