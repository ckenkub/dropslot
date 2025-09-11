#!/usr/bin/env bash
set -euo pipefail

# Start LocalStack in the background
cd "$(dirname "$0")"
COMPOSE=${COMPOSE:-docker compose}
$COMPOSE up -d --pull always

# Wait for LocalStack health (v3 endpoint first, fallback to legacy)
until curl -sf http://localhost:4566/_localstack/health | jq -e '.services' >/dev/null \
  || curl -sf http://localhost:4566/health | jq -e '.services' >/dev/null; do
  echo "Waiting for LocalStack..."; sleep 2; done

echo "LocalStack is up."

# Create ECR repos for this project (mirrors actual AWS ECR usage)
if command -v aws >/dev/null 2>&1; then
  AWS="aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack"
  repos=(api-gateway store-service user-service)
  for r in "${repos[@]}"; do
    $AWS ecr create-repository --repository-name "$r" >/dev/null 2>&1 || true
  done
  echo "ECR repos created: ${repos[*]}"
else
  if command -v docker >/dev/null 2>&1; then
    echo "aws CLI not found; using dockerized aws-cli for ECR setup..."
    IMAGE=amazon/aws-cli:2.17.59 ENDPOINT=http://localhost:4566 REGION=us-east-1 PROFILE=localstack \
      ./awsls.sh ecr describe-repositories >/dev/null 2>&1 || true
    repos=(api-gateway store-service user-service)
    for r in "${repos[@]}"; do
      ./awsls.sh ecr create-repository --repository-name "$r" >/dev/null 2>&1 || true
    done
    echo "ECR repos created (dockerized aws-cli): ${repos[*]}"
  else
    echo "Neither aws CLI nor docker found; skipping ECR repo creation."
  fi
fi

echo "Done. Use AWS CLI with: aws --endpoint-url=http://localhost:4566 --profile localstack <service> ..."
