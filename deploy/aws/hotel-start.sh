#!/bin/bash
# Runs ON the EC2 instance (installed at /usr/local/bin/hotel-start by provision.sh / redeploy.sh).
# Pulls the latest image and (re)starts the container. Every parameter under /hotel/env in SSM Parameter Store
# becomes an environment variable of the container (JWT_SECRET, DB_URL, DB_USERNAME, DB_PASSWORD, ...).
set -euo pipefail

TOKEN="$(curl -s -X PUT http://169.254.169.254/latest/api/token -H 'X-aws-ec2-metadata-token-ttl-seconds: 60')"
REGION="$(curl -s -H "X-aws-ec2-metadata-token: $TOKEN" http://169.254.169.254/latest/meta-data/placement/region)"
ACCOUNT="$(aws sts get-caller-identity --query Account --output text --region "$REGION")"
REGISTRY="$ACCOUNT.dkr.ecr.$REGION.amazonaws.com"
IMAGE="$REGISTRY/hotel-reservations:latest"

ENV_FILE="$(mktemp)"
trap 'rm -f "$ENV_FILE"' EXIT
chmod 600 "$ENV_FILE"
aws ssm get-parameters-by-path --region "$REGION" --path /hotel/env --with-decryption \
  --query 'Parameters[].[Name,Value]' --output text \
  | awk -F'\t' '{ name = $1; sub(".*/", "", name); print name "=" $2 }' > "$ENV_FILE"
echo "JAVA_TOOL_OPTIONS=-Xmx400m" >> "$ENV_FILE"

aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "$REGISTRY"
docker pull "$IMAGE"
docker rm -f hotel 2>/dev/null || true
docker run -d --name hotel --restart always -p 8080:8080 --env-file "$ENV_FILE" "$IMAGE"
