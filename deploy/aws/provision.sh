#!/usr/bin/env bash
# Provisions the backend on AWS: ECR image + one EC2 t3.micro running the container.
# Usage: AWS_PROFILE=hotel ./deploy/aws/provision.sh
# Settings: put DB_URL, DB_USERNAME and DB_PASSWORD in deploy/aws/secrets.env (git-ignored) or export them.
# Safe to re-run: every resource is created only if it does not exist yet.
set -euo pipefail
export MSYS_NO_PATHCONV=1 # keeps Git Bash on Windows from rewriting /paths in arguments

DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$DIR/secrets.env" ]; then set -a; . "$DIR/secrets.env"; set +a; fi

REGION="${AWS_REGION:-us-east-1}"
APP="hotel-reservations"
ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
IMAGE="$REGISTRY/$APP:latest"
ROLE="hotel-backend-ec2-role"
SG_NAME="hotel-backend-sg"
export AWS_DEFAULT_REGION="$REGION"

put_param() { # name value type
  aws ssm put-parameter --name "/hotel/env/$1" --value "$2" --type "$3" --overwrite >/dev/null
  echo "  parameter $1 stored"
}

echo "Account $ACCOUNT, region $REGION"

# 1. Container image ---------------------------------------------------------
aws ecr describe-repositories --repository-names "$APP" >/dev/null 2>&1 \
  || aws ecr create-repository --repository-name "$APP" --image-scanning-configuration scanOnPush=true >/dev/null
aws ecr get-login-password | docker login --username AWS --password-stdin "$REGISTRY"
docker build --platform linux/amd64 -t "$IMAGE" .
docker push "$IMAGE"

# 2. Configuration in SSM Parameter Store (secrets never touch the repo) -----------
# Older versions stored the secret at /hotel/jwt-secret; carry it over so issued tokens stay valid.
if ! aws ssm get-parameter --name /hotel/env/JWT_SECRET >/dev/null 2>&1; then
  LEGACY="$(aws ssm get-parameter --name /hotel/jwt-secret --with-decryption \
    --query Parameter.Value --output text 2>/dev/null || true)"
  put_param JWT_SECRET "${LEGACY:-$(openssl rand -base64 48 | tr -d '\n')}" SecureString
fi
aws ssm get-parameter --name /hotel/env/CORS_ALLOWED_ORIGINS >/dev/null 2>&1 \
  || put_param CORS_ALLOWED_ORIGINS "http://localhost:5173" String
[ -z "${DB_URL:-}" ]      || put_param DB_URL "$DB_URL" String
[ -z "${DB_USERNAME:-}" ] || put_param DB_USERNAME "$DB_USERNAME" String
[ -z "${DB_PASSWORD:-}" ] || put_param DB_PASSWORD "$DB_PASSWORD" SecureString
[ -z "${ADMIN_EMAIL:-}" ]    || put_param ADMIN_EMAIL "$ADMIN_EMAIL" String
[ -z "${ADMIN_PASSWORD:-}" ] || put_param ADMIN_PASSWORD "$ADMIN_PASSWORD" SecureString

# 3. IAM role for the instance (pull from ECR, read parameters, SSM agent) ----
if ! aws iam get-role --role-name "$ROLE" >/dev/null 2>&1; then
  aws iam create-role --role-name "$ROLE" --assume-role-policy-document \
    '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}' >/dev/null
  aws iam attach-role-policy --role-name "$ROLE" --policy-arn arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
  aws iam attach-role-policy --role-name "$ROLE" --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
  aws iam create-instance-profile --instance-profile-name "$ROLE" >/dev/null
  aws iam add-role-to-instance-profile --instance-profile-name "$ROLE" --role-name "$ROLE"
  echo "Waiting for the instance profile to propagate..."
  sleep 15
fi
aws iam put-role-policy --role-name "$ROLE" --policy-name read-hotel-parameters --policy-document \
  "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Action\":[\"ssm:GetParameter\",\"ssm:GetParametersByPath\"],\"Resource\":[\"arn:aws:ssm:$REGION:$ACCOUNT:parameter/hotel/env\",\"arn:aws:ssm:$REGION:$ACCOUNT:parameter/hotel/env/*\"]}]}"

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
  START_SCRIPT="$(tr -d '\r' < "$DIR/hotel-start.sh")"
  USER_DATA="$(cat <<EOF
#!/bin/bash
set -euo pipefail
dnf install -y docker
systemctl enable --now docker
cat > /usr/local/bin/hotel-start <<'SCRIPT'
$START_SCRIPT
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
echo "An existing instance keeps its old start script: run ./deploy/aws/redeploy.sh to update it."
