# Local AWS (zero-cost) with LocalStack

This setup lets you practice AWS workflows without incurring charges by emulating core services locally.

## What you get
- Emulated services: S3, Secrets Manager, SSM, STS, IAM, ECR, ECS, Lambda, API Gateway, EventBridge, CloudWatch/Logs
- Works with your existing Docker build flow and k8s manifests for local testing

## Start/stop

```bash
./localstack/start.sh
# ... later
./localstack/stop.sh
```

## AWS CLI usage
Use the endpoint flag and a dedicated profile so you never hit real AWS:

```bash
aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack sts get-caller-identity
```

You can `aws configure --profile localstack` and leave keys blank; LocalStack accepts any credentials.

No awscli installed? Use the dockerized helper:

```bash
./localstack/awsls.sh sts get-caller-identity
./localstack/awsls.sh s3 mb s3://demo-bucket
```

Run the Day 1 mini-lab script:

```bash
./localstack/day1.sh
```

## ECR + images
After `start.sh`, ECR repos are created. Tag and push your local images to LocalStack ECR:

```bash
export AWS_ENDPOINT=http://localhost:4566
export AWS_REGION=us-east-1
export AWS_PROFILE=localstack

aws --endpoint-url=$AWS_ENDPOINT ecr get-login-password \
  | docker login --username AWS --password-stdin 000000000000.dkr.ecr.$AWS_REGION.localhost.localstack.cloud

docker tag dropslot/api-gateway:latest 000000000000.dkr.ecr.$AWS_REGION.localhost.localstack.cloud/api-gateway:dev

docker push 000000000000.dkr.ecr.$AWS_REGION.localhost.localstack.cloud/api-gateway:dev
```

Tip: You can also keep using plain `docker` images in k8s (no need to pull from ECR) and use LocalStack for S3/Secrets Manager, etc.

Note: The docker-compose `version` key is intentionally omitted (Compose V2 ignores it).

## mapping to the learning path
- Milestone 1 (IAM/S3): use LocalStack S3/IAM
- Milestone 4 (ECR): push to LocalStack ECR
- Milestone 7 (API Gateway): emulate with LocalStack API Gateway
- Milestone 9 (IaC): point Terraform/CloudFormation providers to LocalStack endpoints

For EKS/MSK (Milestones 5–6), keep using your local Kubernetes and the provided Kafka manifest to avoid costs.

## safety
- LocalStack runs only locally; no AWS charges
- Always pass `--endpoint-url=http://localhost:4566` to avoid accidental real calls


AWS terms in simple words
IAM: Identity and Access Management. Who can do what in your AWS account (users, roles, permissions). In this repo: services use roles to read secrets, pull images, etc.
S3: Simple Storage Service. A folder-in-the-cloud for files/objects. In this repo: store build artifacts, logs, backups.
Secrets Manager: Safe vault for passwords/tokens. In this repo: replace postgres-*-secret.yaml with Secrets Manager.
SSM Parameter Store: Key–value config store (can hold simple secrets). In this repo: store non-secret configs like URLs or feature flags.
ECR: Elastic Container Registry. Private Docker image registry. In this repo: push your api-gateway, user-service, store-service images here.
EKS: Elastic Kubernetes Service. Managed Kubernetes control plane. In this repo: run manifests from k8s on AWS instead of local cluster.
MSK: Managed Streaming for Apache Kafka. AWS-managed Kafka. In this repo: replace kafka-dev.yaml with MSK.
API Gateway: Managed “front door” for APIs (routing, auth, throttling). In this repo: optional alternative to an Ingress/ALB in front of services.
CloudWatch: Logs, metrics, and alarms. In this repo: ship app logs/metrics to CloudWatch for dashboards and alerts.
RDS: Relational Database Service. Managed Postgres/MySQL. In this repo: replace postgres-*.yaml with RDS PostgreSQL.
VPC: Virtual Private Cloud. Your private network in AWS. In this repo: EKS, RDS, and MSK live in your VPC subnets.
EC2: Elastic Compute Cloud. Virtual machines. In this repo: sometimes used as jump/bastion hosts or for simple workloads.
ALB (Elastic Load Balancer): Distributes HTTP(S) traffic to services. In this repo: used by EKS via AWS Load Balancer Controller to expose your APIs.
Route 53: DNS. Human-friendly names for your endpoints. In this repo: map a domain to your ALB/API Gateway.
Lambda: Serverless functions (no servers to manage). In this repo: not required, but useful for small jobs or webhooks.
SQS: Simple Queue Service. Message queue. In this repo: alternative to Kafka for simple async messaging.