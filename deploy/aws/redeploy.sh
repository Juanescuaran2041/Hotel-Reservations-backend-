#!/usr/bin/env bash
# Rebuilds and pushes the image, then restarts the container on the EC2 instance through SSM.
# Any of CORS_ALLOWED_ORIGINS, DB_URL, DB_USERNAME, DB_PASSWORD, ADMIN_EMAIL, ADMIN_PASSWORD that is set
# (exported, or written in deploy/aws/secrets.env which is git-ignored) is stored in SSM first.
# Usage: AWS_PROFILE=hotel ./deploy/aws/redeploy.sh [--config-only]
set -euo pipefail
export MSYS_NO_PATHCONV=1 # keeps Git Bash on Windows from rewriting /paths in arguments

DIR="$(cd "$(dirname "$0")" && pwd)"
if [ -f "$DIR/secrets.env" ]; then set -a; . "$DIR/secrets.env"; set +a; fi

REGION="${AWS_REGION:-us-east-1}"
APP="hotel-reservations"
ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
REGISTRY="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
export AWS_DEFAULT_REGION="$REGION"

store() { # name type
  local value="${!1:-}"
  [ -z "$value" ] && return 0
  aws ssm put-parameter --name "/hotel/env/$1" --value "$value" --type "$2" --overwrite >/dev/null
  echo "Stored $1"
}
store CORS_ALLOWED_ORIGINS String
store DB_URL String
store DB_USERNAME String
store DB_PASSWORD SecureString
store ADMIN_EMAIL String
store ADMIN_PASSWORD SecureString

if [ "${1:-}" != "--config-only" ]; then
  aws ecr get-login-password | docker login --username AWS --password-stdin "$REGISTRY"
  docker build --platform linux/amd64 -t "$REGISTRY/$APP:latest" .
  docker push "$REGISTRY/$APP:latest"
fi

INSTANCE_ID="$(aws ec2 describe-instances \
  --filters Name=tag:Name,Values="$APP" Name=instance-state-name,Values=running \
  --query 'Reservations[0].Instances[0].InstanceId' --output text)"

# Always refresh the start script on the instance so it matches the repository.
SCRIPT_B64="$(tr -d '\r' < "$DIR/hotel-start.sh" | base64 -w0)"
COMMAND_ID="$(aws ssm send-command --instance-ids "$INSTANCE_ID" --document-name AWS-RunShellScript \
  --parameters "commands=[\"echo $SCRIPT_B64 | base64 -d > /usr/local/bin/hotel-start\",\"chmod +x /usr/local/bin/hotel-start\",\"/usr/local/bin/hotel-start\"]" \
  --query Command.CommandId --output text)"
aws ssm wait command-executed --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID"
aws ssm get-command-invocation --command-id "$COMMAND_ID" --instance-id "$INSTANCE_ID" \
  --query '[Status,StandardErrorContent]' --output text
echo "Container restarted on $INSTANCE_ID (the application needs about a minute to boot)"
