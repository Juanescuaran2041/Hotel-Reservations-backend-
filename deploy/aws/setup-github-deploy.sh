#!/usr/bin/env bash
# One-time setup that lets GitHub Actions deploy without stored AWS keys (OIDC).
# Creates the GitHub identity provider and one least-privilege role per repository:
#   - backend role:  push the image to ECR and restart the container through SSM
#   - frontend role: upload the site to S3 and invalidate CloudFront
# Only workflows running on the main branch of each repository can assume its role.
# Requires the GitHub CLI (gh) logged in. Re-run it if a repository is renamed or recreated.
# Usage: AWS_PROFILE=hotel GITHUB_OWNER=Juanescuaran2041 ./deploy/aws/setup-github-deploy.sh
set -euo pipefail
export MSYS_NO_PATHCONV=1 # keeps Git Bash on Windows from rewriting /paths in arguments

OWNER="${GITHUB_OWNER:?Set GITHUB_OWNER to the GitHub user or organization}"
BACKEND_REPO="${BACKEND_REPO:-Hotel-Reservations-backend-}"
FRONTEND_REPO="${FRONTEND_REPO:-Hotel-Reservations-Frontend}"
REGION="${AWS_REGION:-us-east-1}"
ACCOUNT="$(aws sts get-caller-identity --query Account --output text)"
PROVIDER_ARN="arn:aws:iam::$ACCOUNT:oidc-provider/token.actions.githubusercontent.com"
BUCKET="hotel-frontend-$ACCOUNT"
export AWS_DEFAULT_REGION="$REGION"

aws iam get-open-id-connect-provider --open-id-connect-provider-arn "$PROVIDER_ARN" >/dev/null 2>&1 \
  || aws iam create-open-id-connect-provider --url https://token.actions.githubusercontent.com \
       --client-id-list sts.amazonaws.com --thumbprint-list 6938fd4d98bab03faadb97b34396831e3780aea1 >/dev/null

# GitHub identifies a repository in the OIDC token as "repo:owner@<id>/name@<id>" (immutable, survives renames).
# The exact prefix is read from the GitHub API; the plain "repo:owner/name" form is only a fallback.
subject_prefix() {
  gh api "repos/$OWNER/$1/actions/oidc/customization/sub" -q .sub_claim_prefix 2>/dev/null || echo "repo:$OWNER/$1"
}

create_role() { # role-name repo policy-json
  local prefix; prefix="$(subject_prefix "$2")"
  local trust="{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Federated\":\"$PROVIDER_ARN\"},\"Action\":\"sts:AssumeRoleWithWebIdentity\",\"Condition\":{\"StringEquals\":{\"token.actions.githubusercontent.com:aud\":\"sts.amazonaws.com\",\"token.actions.githubusercontent.com:sub\":\"$prefix:ref:refs/heads/main\"}}}]}"
  if aws iam get-role --role-name "$1" >/dev/null 2>&1; then
    aws iam update-assume-role-policy --role-name "$1" --policy-document "$trust"
  else
    aws iam create-role --role-name "$1" --assume-role-policy-document "$trust" >/dev/null
  fi
  aws iam put-role-policy --role-name "$1" --policy-name deploy --policy-document "$3"
  echo "$1: arn:aws:iam::$ACCOUNT:role/$1"
}

BACKEND_POLICY="{\"Version\":\"2012-10-17\",\"Statement\":[
 {\"Effect\":\"Allow\",\"Action\":\"ecr:GetAuthorizationToken\",\"Resource\":\"*\"},
 {\"Effect\":\"Allow\",\"Action\":[\"ecr:BatchCheckLayerAvailability\",\"ecr:BatchGetImage\",\"ecr:CompleteLayerUpload\",\"ecr:InitiateLayerUpload\",\"ecr:PutImage\",\"ecr:UploadLayerPart\"],\"Resource\":\"arn:aws:ecr:$REGION:$ACCOUNT:repository/hotel-reservations\"},
 {\"Effect\":\"Allow\",\"Action\":\"ec2:DescribeInstances\",\"Resource\":\"*\"},
 {\"Effect\":\"Allow\",\"Action\":\"ssm:SendCommand\",\"Resource\":[\"arn:aws:ssm:$REGION::document/AWS-RunShellScript\",\"arn:aws:ec2:$REGION:$ACCOUNT:instance/*\"]},
 {\"Effect\":\"Allow\",\"Action\":[\"ssm:GetCommandInvocation\",\"ssm:ListCommandInvocations\"],\"Resource\":\"*\"}]}"

FRONTEND_POLICY="{\"Version\":\"2012-10-17\",\"Statement\":[
 {\"Effect\":\"Allow\",\"Action\":\"s3:ListBucket\",\"Resource\":\"arn:aws:s3:::$BUCKET\"},
 {\"Effect\":\"Allow\",\"Action\":[\"s3:GetObject\",\"s3:PutObject\",\"s3:DeleteObject\"],\"Resource\":\"arn:aws:s3:::$BUCKET/*\"},
 {\"Effect\":\"Allow\",\"Action\":[\"cloudfront:CreateInvalidation\",\"cloudfront:ListDistributions\",\"cloudfront:GetDistribution\"],\"Resource\":\"*\"}]}"

create_role hotel-github-backend-deploy "$BACKEND_REPO" "$BACKEND_POLICY"
create_role hotel-github-frontend-deploy "$FRONTEND_REPO" "$FRONTEND_POLICY"
echo
echo "S3 bucket: $BUCKET"
echo "CloudFront distribution: $(aws cloudfront list-distributions \
  --query "DistributionList.Items[?Comment=='hotel-reservations'].Id | [0]" --output text)"
