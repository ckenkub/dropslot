# Local Kubernetes Dev with Skaffold

This guide shows how to run DropSlot backend services on a local Kubernetes (Rancher Desktop/Minikube) using Skaffold and Jib.

## Prereqs
- kubectl configured to a local cluster
- Skaffold v2+
- Docker/Podman and JDK 21

## One-time setup
- Create JWT secret (used by user-service):

```bash
kubectl create secret generic jwt-secret --from-literal=secret=change-me
```

## Start dev loop

```bash
skaffold dev
```

Skaffold will:
- Build images via Jib for:
  - discovery-service (Eureka)
  - api-gateway (Spring Cloud Gateway)
  - user-service
  - store-service
- Deploy Kubernetes manifests in `k8s/`
- Port-forward services to localhost

## Endpoints
- API Gateway: http://localhost:8080
- Eureka: http://localhost:8761
- User Service: http://localhost:8081
- Store Service: http://localhost:8082

## Gateway routes
- /api/auth/** and /api/users/** → user-service
- /api/stores/** → store-service

## Notes
- In k8s profile, services use in-cluster Postgres (`postgres-user`, `postgres-store`).
- Health probe endpoints are enabled at `/actuator/health/{liveness|readiness}`.
- For Docker Compose workflows, Postgres is still available under `backend/docker-compose.yml`.
