#!/usr/bin/env bash
set -euo pipefail

# Day 1 mini-lab script for LocalStack
# - Creates an S3 bucket, enables versioning, uploads a test file
# - Creates a Secrets Manager secret and reads it back
# - Optionally syncs the secret into Kubernetes
# - Ensures ECR repos exist and tries to log in to LocalStack ECR

AWS_ENDPOINT=${AWS_ENDPOINT:-http://localhost:4566}
AWS_REGION=${AWS_REGION:-us-east-1}
AWS_PROFILE=${AWS_PROFILE:-localstack}

# pick aws command: native aws if available, otherwise use local awsls.sh helper
if command -v aws >/dev/null 2>&1; then
  aws_cmd(){ aws --endpoint-url="$AWS_ENDPOINT" --region "$AWS_REGION" --profile "$AWS_PROFILE" "$@"; }
elif [ -x "$(dirname "$0")/awsls.sh" ]; then
  aws_cmd(){ "$(dirname "$0")/awsls.sh" "$@"; }
else
  echo "ERROR: no aws CLI found and no awsls.sh helper. Install awscli or ensure awsls.sh is present." >&2
  exit 1
fi

echo "Using endpoint: $AWS_ENDPOINT, region: $AWS_REGION, profile: $AWS_PROFILE"

# 1) S3
BUCKET="dropslot-$(whoami)-day1"
echo "\n== S3: creating bucket $BUCKET and uploading a test file"
aws_cmd s3 mb s3://$BUCKET >/dev/null 2>&1 || true
aws_cmd s3api put-bucket-versioning --bucket $BUCKET --versioning-configuration Status=Enabled >/dev/null 2>&1 || true
printf 'hello day1\n' > /tmp/hello_day1.txt
aws_cmd s3 cp /tmp/hello_day1.txt s3://$BUCKET/notes/hello.txt
echo "Objects in $BUCKET:"
aws_cmd s3 ls s3://$BUCKET --recursive || true

# 2) Secrets Manager
echo "\n== Secrets Manager: create/read secret dropslot/db/password"
# try create, if exists put a new value
if aws_cmd secretsmanager create-secret --name dropslot/db/password --secret-string 'password' >/dev/null 2>&1; then
  echo "Secret created."
else
  echo "Secret may already exist; updating value..."
  aws_cmd secretsmanager put-secret-value --secret-id dropslot/db/password --secret-string 'password' >/dev/null 2>&1 || true
fi

echo "Secret value:"
aws_cmd secretsmanager get-secret-value --secret-id dropslot/db/password | jq -r .SecretString || true

# 3) Optional: sync to Kubernetes
if command -v kubectl >/dev/null 2>&1; then
  echo "\n== Kubernetes: syncing secret into cluster as 'demo-db-secret'"
  DB_PASS=$(aws_cmd secretsmanager get-secret-value --secret-id dropslot/db/password | jq -r .SecretString)
  kubectl create secret generic demo-db-secret --from-literal=password="$DB_PASS" --dry-run=client -o yaml | kubectl apply -f -
  echo "Kubernetes secret created:"; kubectl get secret demo-db-secret -o yaml
else
  echo "\n== kubectl not found: skipping k8s secret sync"
fi

# 4) ECR: ensure repos and login
if command -v docker >/dev/null 2>&1; then
  echo "\n== ECR: ensuring repositories and attempting docker login"
  repos=(api-gateway store-service user-service)
  for r in "${repos[@]}"; do
  aws_cmd ecr create-repository --repository-name "$r" >/dev/null 2>&1 || true
  done
  REGISTRY="000000000000.dkr.ecr.$AWS_REGION.localhost.localstack.cloud"
  echo "Logging into registry $REGISTRY"
  aws_cmd ecr get-login-password | docker login --username AWS --password-stdin $REGISTRY || true
  echo "ECR login done. To push: docker tag <image> $REGISTRY/<repo>:<tag> && docker push ..."
else
  echo "\n== docker not found: skipping ECR setup/login"
fi

cat <<EOF

Day 1 lab complete.
Summary:
 - S3 bucket: $BUCKET (uploaded /notes/hello.txt)
 - Secret: dropslot/db/password (value 'password')
 - Kubernetes secret: demo-db-secret (if kubectl present)
 - ECR repos: api-gateway, store-service, user-service (if docker present)

Cleanup commands (optional):
 aws --endpoint-url=$AWS_ENDPOINT --region $AWS_REGION --profile $AWS_PROFILE s3 rb s3://$BUCKET --force
 aws --endpoint-url=$AWS_ENDPOINT --region $AWS_REGION --profile $AWS_PROFILE secretsmanager delete-secret --secret-id dropslot/db/password --force-delete-without-recovery

EOF
