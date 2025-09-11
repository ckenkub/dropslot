#!/usr/bin/env bash
set -euo pipefail

# Run AWS CLI in a container configured for LocalStack
# Usage: ./awsls.sh <aws-args...>

IMAGE=${IMAGE:-amazon/aws-cli:2.17.59}
ENDPOINT=${ENDPOINT:-http://localhost:4566}
REGION=${REGION:-us-east-1}
PROFILE=${PROFILE:-localstack}

exec docker run --rm \
  -e AWS_ACCESS_KEY_ID=test \
  -e AWS_SECRET_ACCESS_KEY=test \
  -e AWS_DEFAULT_REGION=$REGION \
  -v "$PWD:/work" -w /work \
  --network host \
  $IMAGE \
  aws --endpoint-url "$ENDPOINT" --region "$REGION" --profile "$PROFILE" "$@"
