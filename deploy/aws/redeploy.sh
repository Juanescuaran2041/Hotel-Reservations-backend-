#!/usr/bin/env bash
# Rebuilds and pushes the image, then restarts the container on the EC2 instance through SSM.
# Optional: CORS_ALLOWED_ORIGINS="https://dxxxx.cloudfront.net" to update the allowed origins first.
# Usage: AWS_PROFILE=hotel ./deploy/aws/redeploy.sh [--config-only]
set -euo pipefail
export MSYS_NO_PATHCONV=1 # keeps Git Bash on Windows from rewriting /paths in arguments

REGION="${AWS_REGION:-us-east-1}"
APP="hotel-reservations"
ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
export AWS_DEFAULT_REGION="$REGION"

if [ -n "${CORS_ALLOWED_ORIGINS:-}" ]; then
  aws ssm put-parameter --name /hotel/cors-allowed-origins --type String --overwrite \
    --value "$CORS_ALLOWED_ORIGINS" >/dev/null
  echo "Allowed origins set to $CORS_ALLOWED_ORIGINS"
fi

if [ "${1:-}" != "--config-only" ]; then
  aws ecr get-login-password | docker login --username AWS --password-stdin "$REGISTRY"
  docker build --platform linux/amd64 -t "$REGISTRY/$APP:latest" .
  docker push "$REGISTRY/$APP:latest"
fi

INSTANCE_ID="$(aws ec2 describe-instances \
  --filters Name=tag:Name,Values="$APP" Name=instance-state-name,Values=running \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)"
COMMAND_ID="$(aws ssm send-command --instance-ids "$INSTANCE_ID" --document-name AWS-RunShellScript \
  --parameters 'commands=["/usr/local/bin/hotel-start"]' --query Command.CommandId --output text)"
aws ssm wait command-executed --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID"
echo "Container restarted on $INSTANCE_ID"
