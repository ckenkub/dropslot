# DropSlot — Full-Stack Application Requirements (Master Spec)

This is the **complete requirements specification** for the DropSlot project, including project overview, features, advanced features, API conventions, database schema, non-functional requirements, Rancher Desktop setup, and delivery plan.

---

## 1. Project Overview
**Domain:** E-commerce / Reservations
**Purpose:** Allow brands or stores to schedule limited product "drops" (e.g., collectibles, sneakers, art toys) with time-boxed booking slots. Customers can reserve slots, join waitlists, receive notifications, and check in with QR codes. Admins and managers handle store setup, drops, capacity, reservations, and analytics.

---

## 2. User Roles
- **Guest** – browse drops, register/login
- **Customer** – profile, reservations, waitlist, notifications
- **Store Manager** – manage stores, products, drops, slots, and perform check-ins
- **Admin** – user management, global configs, analytics, audit logs
- **Support (read-only)** – view reservations/logs, assist customers

---

## 3. Core Features (MVP)
1. **Auth & RBAC**: JWT/refresh tokens, password reset, email verification, role-based access.  
2. **Stores & Branches**: CRUD for stores and physical branches.  
3. **Products & Drops**: CRUD with slots (date/time capacities).  
4. **Reservations**: State transitions (PENDING → CONFIRMED → CHECKED_IN/CANCELLED/EXPIRED).  
5. **Waitlist**: Join when full, auto-promote when capacity frees up.  
6. **Notifications**: Email confirmations/cancellations, optional PWA push.  
7. **Search & Filters**: Filter drops by product, branch, date/time.  
8. **Basic Analytics**: Fill rate, no-show rate, reservation counts.  
9. **Audit Log**: Record all admin/manager actions.  

---

## 4. Advanced Features
- Payments (Stripe/Omise) with refunds.  
- Real-time slot counters via WebSockets/SSE.  
- Calendar integration (iCal, Google/Apple).  
- External integrations: LINE Notify, Twilio SMS, SendGrid, Slack.  
- Webhooks for external systems (reservation.created, cancelled).  
- Promo/early access codes.  
- Multi-tenant support.  
- Mobile check-in PWA.  
- Advanced analytics: cohorts, conversion funnels, predictors.  

---

## 5. Tech Stack Suggestions

### Frontend
- **Next.js 14 (React, App Router, RSC)**  
- TypeScript, Tailwind CSS, React Hook Form + Zod  
- TanStack Query (server cache)  
- NextAuth.js for auth  
- PWA with push notifications  

### Backend
- **Spring Boot 3 (Java 21)**  
- PostgreSQL 16 (default) or Oracle 19c (alt)  
- Redis for caching/rate limiting  
- Liquibase for migrations (enterprise-grade with rollback support)  
- MapStruct for DTO mapping  
- Kafka/RabbitMQ for async flows

### DevOps
- Docker + Skaffold for local builds  
- Kubernetes via Rancher Desktop (single-node k8s)  
- CI/CD: GitHub Actions  
- OpenAPI 3 docs with springdoc  
- Observability: Prometheus, Grafana, OpenTelemetry  
- Secrets: Vault or K8s secrets  

---

## 6. API Endpoints

### Conventions
- Pagination: `?page=1&size=20&sort=field,asc`  
- Errors: RFC 7807 Problem JSON  
- Idempotency: `Idempotency-Key` header on POSTs  

### Auth
- `POST /api/v1/auth/register`  
- `POST /api/v1/auth/login`  
- `POST /api/v1/auth/refresh`  
- `POST /api/v1/auth/password/reset`  
- `POST /api/v1/auth/verify-email`  

### Users
- `GET /api/v1/me`  
- `PATCH /api/v1/me`  
- `GET /api/v1/users` (admin)  
- `PATCH /api/v1/users/{id}/roles` (admin)  

### Stores & Branches
- `GET /api/v1/stores` / `POST /api/v1/stores`  
- `GET /api/v1/branches` / `POST /api/v1/branches`  

### Products
- `GET /api/v1/products` / `POST /api/v1/products`  
- `GET /api/v1/products/{id}` / `PATCH` / `DELETE`  

### Drops & Slots
- `GET /api/v1/drops` / `POST /api/v1/drops`  
- `GET /api/v1/drops/{id}/slots`  
- `POST /api/v1/drops/{id}/slots` (manager)  

### Reservations & Waitlist
- `POST /api/v1/reservations`  
- `POST /api/v1/reservations/{id}/cancel`  
- `POST /api/v1/waitlist`  
- `GET /api/v1/waitlist?dropId=`  

### Tickets & Check-in
- `GET /api/v1/tickets/{reservationId}/qr`  
- `POST /api/v1/checkin` (manager)  

### Admin & Analytics
- `GET /api/v1/admin/metrics`  
- `GET /api/v1/admin/audit-logs`  

---

## 7. Database Schema (Detailed)

**users**  
- id (UUID, PK), email (uniq), password_hash, name, status, timestamps, email_verified_at  

**roles**  
- id, code (uniq), name  

**user_roles**  
- user_id (FK), role_id (FK), PK (user_id, role_id)  

**stores**  
- id, name, slug (uniq), tenant_key, logo_url, created_by, timestamps  

**branches**  
- id, store_id (FK), name, address, lat, lng, phone, opening_hours (JSON)  

**products**  
- id, store_id (FK), name, sku (uniq per store), description, images (JSON), price_cents, currency, is_active  

**drops**  
- id, store_id (FK), product_id (FK), name, starts_at, ends_at, reservation_rule (JSON), status  

**slots**  
- id, drop_id (FK), start_time, end_time, capacity_total, capacity_reserved, capacity_waitlist  

**reservations**  
- id, slot_id (FK), user_id (FK), state, qr_code_url, note, timestamps  
- Unique (user_id, slot_id); enforce one active per drop at service layer  

**waitlist**  
- id, slot_id (FK), user_id (FK), position, state, timestamps  
- Unique (user_id, slot_id)  

**payments**  
- id, reservation_id (FK, unique), provider, provider_intent_id, amount_cents, currency, status, timestamps  

**notifications**  
- id, user_id (FK), channel, template, payload, status, timestamps  

**webhook_endpoints**  
- id, store_id (FK), url, secret, active, timestamps  

**webhook_events**  
- id, endpoint_id (FK), type, payload, status, retry_count, timestamps  

**audit_logs**  
- id, actor_user_id (FK), action, entity, entity_id, before (JSON), after (JSON), ip, ua, created_at  

---

## 8. Non-Functional Requirements

### Security
- OWASP ASVS L2, input validation, output encoding  
- Password hashing (bcrypt/Argon2id)  
- JWT short TTL + refresh, CSRF (if cookies)  
- Row-level security (tenant isolation)  
- Signed URLs for QR/tickets, webhook signatures  
- Rate limiting per IP/device/user  
- Audit logging of privileged actions  

### Performance & Scalability
- p95 API latency < 300ms cached / < 800ms uncached  
- Horizontal scaling with k8s pods  
- Redis caching for availability queries  
- Indexed queries (drop_id, slot_id, user_id)  

### Availability & Resilience
- 99.9% uptime target  
- Rolling deployments, health probes  
- Idempotency keys on resource creation  
- Outbox pattern for reliable notifications/webhooks  

### Observability
- Structured JSON logs with correlation IDs  
- OpenTelemetry tracing across FE/BE  
- Prometheus/Grafana dashboards  

### Maintainability
- Codegen from OpenAPI specs  
- DTO mapping with MapStruct  
- Prettier/ESLint/Spotless for code style  

### Compliance & Accessibility
- WCAG 2.1 AA  
- GDPR/PDPA compliance (export/delete user data)  

---

## 9. Local Dev & Containerization (Rancher Desktop)

- **Runtime:** Rancher Desktop (containerd/dockerd)  
- **Kubernetes:** Rancher Desktop single-node k8s  
- **Dev Loop:** Skaffold (`skaffold dev`) + Jib for Java images  
- **Networking:** backend on `localhost:8080`, frontend on `localhost:3000`  
- **Persistence:** local Postgres volume (ephemeral by default)  
- **Optional services:**  
  - PlantUML server (Docker)  
    ```yaml
    services:
      plantuml:
        image: plantuml/plantuml-server:jetty
        ports:
          - "8081:8080"
    ```

---

## 10. Suggested Delivery Plan

1. **MVP Iteration 1** – Auth, Stores/Branches, Products, Drops/Slots  
2. **MVP Iteration 2** – Reservations, Waitlist, Notifications, QR Tickets  
3. **Ops Iteration** – CI/CD, logging, metrics, audit log  
4. **Advanced Iteration** – Payments, real-time updates, webhooks  
5. **Analytics Iteration** – Cohorts, conversion funnels, no-show prediction  

---

## 11. Acceptance Criteria (Examples)
- Customer can register, login, reserve a slot, receive email + QR ticket, and cancel.  
- Manager can create drops/slots, enforce per-user limit, and see capacity update.  
- Waitlisted user is auto-promoted when capacity frees.  
- API is idempotent, paginated, and errors follow RFC 7807.  

---
