# AWS learning path for dropslot

A practical, hands-on path to build real, job-ready AWS skills using this repo (Spring Boot microservices, Kafka, Kubernetes). Progress through the milestones in order; each has clear outcomes and quick checks.

- Who this is for: backend/devops engineer building/deploying microservices
- Time: 2–3 weeks part-time (split into 10 milestones, 30–120 mins each)
- Cost: keep to Free Tier where possible; MSK/EKS can incur costs—tear down when idle

## prerequisites
- Linux/bash, Docker, Git
- AWS account with billing alerts enabled
- Tools installed: awscli v2, kubectl, helm, eksctl, jq, terraform (optional)

Optional but helpful: skaffold, kubens/kubectx

## day 1 mini-lab — IAM, S3, and Secrets (LocalStack, 30–45m)
Outcome: run basic AWS operations locally at zero cost; understand identities, buckets, and secrets.

Prereqs: Docker running, awscli, jq. Start LocalStack:

```bash
./localstack/start.sh
```

1) Configure a safe local profile and test identity

```bash
aws configure --profile localstack
# Access Key ID / Secret can be any value for LocalStack (e.g., test/test)
# Default region: us-east-1

aws --endpoint-url=http://localhost:4566 \
    --region us-east-1 \
    --profile localstack sts get-caller-identity
```

Success: a JSON with an Account/Arn prints (LocalStack emulation).

2) Create and use an S3 bucket

```bash
BUCKET=dropslot-$(whoami)-day1

aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack \
  s3 mb s3://$BUCKET

aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack \
  s3api put-bucket-versioning \
  --bucket $BUCKET \
  --versioning-configuration Status=Enabled

echo "hello day1" > /tmp/hello.txt
aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack \
  s3 cp /tmp/hello.txt s3://$BUCKET/notes/hello.txt

aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack \
  s3 ls s3://$BUCKET/ --recursive
```

Success: you see the uploaded object listed.

3) Store and read a secret (Secrets Manager)

```bash
aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack \
  secretsmanager create-secret \
  --name dropslot/db/password \
  --secret-string 'password'

aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack \
  secretsmanager get-secret-value --secret-id dropslot/db/password | jq -r .SecretString
```

Success: the command prints "password".

Bonus: sync secret into Kubernetes for local services (optional)

```bash
DB_PASS=$(aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack \
  secretsmanager get-secret-value --secret-id dropslot/db/password | jq -r .SecretString)

kubectl create secret generic demo-db-secret \
  --from-literal=password="$DB_PASS" \
  --dry-run=client -o yaml | kubectl apply -f -

kubectl get secret demo-db-secret -o yaml | grep -A2 data:
```

Cleanup (optional):

```bash
aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack \
  s3 rb s3://$BUCKET --force || true
aws --endpoint-url=http://localhost:4566 --region us-east-1 --profile localstack \
  secretsmanager delete-secret --secret-id dropslot/db/password --force-delete-without-recovery || true
./localstack/stop.sh
```

## milestone 0 — account safety and CLI setup (30–45m)
Outcome: secure account, budget guardrails, working AWS CLI profile

- Enable MFA on root; create an Admin IAM user/role; sign in as the admin user
- Create a Budget and two alerts (actual and forecast > $10)
- Configure CLI

```bash
# Option A: SSO (work/organization)
aws configure sso --profile dropslot-dev

# Option B: access keys (personal sandbox)
aws configure --profile dropslot-dev

# Validate
aws sts get-caller-identity --profile dropslot-dev
```

Quick check: see your ARN printed; budgets visible in Billing console.

## milestone 1 — IAM and S3 fundamentals (45–60m)
Outcome: least-privilege practice and object storage basics

- Create an IAM policy for read-only S3 and attach to a new IAM user (or role)
- Create an S3 bucket for app artifacts and logs: `dropslot-<yourid>-artifacts`
- Upload/download, enable versioning, set default encryption

```bash
aws s3 mb s3://dropslot-<yourid>-artifacts --profile dropslot-dev
aws s3api put-bucket-versioning \
  --bucket dropslot-<yourid>-artifacts \
  --versioning-configuration Status=Enabled \
  --profile dropslot-dev
aws s3 cp README.md s3://dropslot-<yourid>-artifacts/ --profile dropslot-dev
```

Quick check: object versions visible; un-versioned delete is blocked via versioning+MFA delete (optional).

## milestone 2 — VPC and EC2 lab (60–90m)
Outcome: networking fundamentals and a managed compute baseline

- Create a VPC with 2 public+2 private subnets and an Internet/NAT gateway (use VPC wizard)
- Launch a small Amazon Linux 2 instance in a public subnet, connect via SSH
- Install Docker; run a hello container; push logs to CloudWatch via the agent (optional)

Quick check: instance reachable; security group rules make sense (ingress 22 from your IP only).

## milestone 3 — RDS PostgreSQL (60–90m)
Outcome: managed database provisioning, connectivity, and credentials hygiene

- Create RDS PostgreSQL in private subnets, enable auto minor version upgrades, backups
- Store credentials in AWS Secrets Manager
- From your EC2 jump host or local with Session Manager port-forward, connect and run a schema

Tip: this repo’s `k8s/postgres-*.yaml` files are a reference; you’ll replace them with RDS later.

Quick check: `psql` connects; a sample table can be created and queried.

## milestone 4 — ECR and container images (30–45m)
Outcome: private registry and image publishing

- Create an ECR repo per service: `api-gateway`, `store-service`, `user-service`
- Log in and push images built from this repo

```bash
aws ecr get-login-password --profile dropslot-dev | \
  docker login --username AWS --password-stdin <account>.dkr.ecr.<region>.amazonaws.com

# Example push (adjust tags/services as needed)
docker build -t api-gateway:dev ./backend/api-gateway
aws ecr create-repository --repository-name api-gateway --profile dropslot-dev || true
docker tag api-gateway:dev <account>.dkr.ecr.<region>.amazonaws.com/api-gateway:dev
docker push <account>.dkr.ecr.<region>.amazonaws.com/api-gateway:dev
```

Quick check: image appears in ECR console with tag `dev`.

## milestone 5 — EKS cluster and first deploy (90–120m)
Outcome: run your Kubernetes manifests on AWS

- Create an EKS cluster (managed node group)

```bash
eksctl create cluster \
  --name dropslot-dev \
  --region <region> \
  --with-oidc \
  --nodes 2 --node-type t3.medium \
  --managed
```

- Install AWS Load Balancer Controller and metrics-server via Helm
- Create a namespace (e.g., `dropslot`), and apply your manifests in `k8s/`
- Replace in-cluster Postgres with RDS by wiring Secrets Manager/Parameter Store and updating `Deployment` env vars

Quick check: `kubectl get pods -n dropslot` shows running; `kubectl get ingress` shows an address; services are reachable.

## milestone 6 — Kafka with Amazon MSK (90–120m)
Outcome: managed Kafka integrated with your services

- Create an MSK cluster (serverless or smallest provisioned)
- Place MSK in the same VPC/subnets used by EKS; create security groups to allow broker access from EKS nodes
- Update microservice configs to use MSK bootstrap brokers; redeploy

Cost note: MSK can be pricey; when idle, stop/tear down. For low-cost practice, consider LocalStack or a single t3.small Kafka on EC2 for learning only.

Quick check: produce/consume a test topic using `kafka-console-producer/consumer` from a toolbox pod in EKS.

## milestone 7 — API, auth, and edge (60–90m)
Outcome: secure entry via ALB Ingress or API Gateway

Option A (typical for EKS):
- Use AWS Load Balancer Controller with an Ingress manifest to route to services
- Attach WAF (optional) and TLS via ACM

Option B (managed edge):
- Front services with API Gateway HTTP API + VPC Link to an NLB targeting your EKS services
- Add JWT auth (Amazon Cognito) if needed—this repo already uses JWT; migrate to Cognito later

Quick check: curl over HTTPS returns a valid response; unauthorized requests are rejected.

## milestone 8 — observability and ops (45–90m)
Outcome: logs, metrics, traces, alarms

- Enable CloudWatch Container Insights for EKS
- Centralize app logs to CloudWatch Logs with structured JSON; create metric filters and alarms
- Optional: deploy OpenTelemetry Collector and AWS X-Ray for traces

Quick check: dashboard shows CPU/mem; log insights queries return app logs; alarm triggers on a synthetic error.

## milestone 9 — infrastructure as code (60–120m)
Outcome: reproducible environments

Choose one:
- CloudFormation (native) or
- Terraform (common in teams)

Start small: a module/stack for VPC + EKS + node group. Add ECR repos and RDS next. Parameterize region, names, and instance sizes. Store state in S3+DynamoDB (for Terraform).

Quick check: `terraform apply` (or stack deploy) creates and destroys infra reliably.

## milestone 10 — CI/CD with GitHub Actions OIDC (60–120m)
Outcome: build, push, and deploy without long-lived AWS keys

- Create an IAM role for GitHub OIDC with trust policy restricted to this repo/branch
- Workflow jobs:
  - Build and push images to ECR
  - Run smoke tests
  - Deploy manifests to EKS (kubectl/helm)

Quick check: a PR triggers a pipeline; images get new tags; rollout happens; smoke test passes.

---

## tie-ins to this repo
- `k8s/`: reuse manifests; add Ingress annotations for AWS Load Balancer Controller; swap DB env to RDS/Secrets Manager
- `backend/*/Dockerfile`: ensure images are small and ECR-ready
- `docs/`: keep notes per milestone; consider a `docs/aws/` subfolder if you expand labs

## teardown and cost hygiene
- Stop MSK/EKS when not used; delete clusters nightly during learning if needed
- Use budgets and daily Cost Explorer checks
- Prefer t3/t4g instance families; keep storage small; enable TTLs/retention where possible

## what’s next
- I can scaffold: EKS helm charts, ALB Ingress, and an initial GitHub Actions OIDC workflow for ECR/EKS
- If you prefer ECS Fargate over EKS, I can produce equivalent task/service definitions

If you want, say “kick off milestone 5” and I’ll generate the exact Helm/manifest edits tying your current `k8s/` to EKS + ALB and RDS.

## zero-cost alternative: LocalStack + local Kubernetes
You can do most learning without costs by emulating AWS locally and using your existing cluster:

- Use LocalStack (see `localstack/README.md`) for S3, IAM, Secrets Manager, SSM, ECR, API Gateway, CloudWatch/Logs.
- Keep EKS/MSK milestones on your local Kubernetes with our existing manifests (Kafka dev stack provided in `k8s/kafka-dev.yaml`).
- For IaC, point Terraform/CloudFormation providers to LocalStack’s endpoints.

Suggested mapping:
- M0/M1: IAM/S3 with LocalStack
- M3: Swap RDS with a local Postgres (k8s manifests already provided)
- M4: ECR via LocalStack
- M5: Use local k8s instead of EKS
- M6: Use `k8s/kafka-dev.yaml` instead of MSK
- M7: API Gateway via LocalStack or Traefik ingress locally
- M8: Use open-source stack locally (Prometheus/Grafana) or LocalStack CloudWatch
- M9/M10: IaC + CI against LocalStack endpoints and local cluster
