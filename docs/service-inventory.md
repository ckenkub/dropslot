# Service Inventory & Phase Map

This page summarizes all backend services and their phase mapping, for quick reference during implementation.

## Services
- API Gateway — Request routing, auth enforcement, docs aggregation
- User Service — Auth, JWT, profiles, RBAC
- Store Service — Stores, branches, settings
- Product Service — Catalog, SKUs, categories, pricing
- Drop Service — Drops, slots, capacity, rules
- Reservation Service — Booking, waitlist, QR check-in
- Notification Service — Email/SMS/push, templates, delivery logs
- Payment Service — Stripe/Omise, intents, refunds
- Analytics Service — Metrics, reports, BI

## Phase Map
- Phase 1 (Foundation):
  - User Service
  - Store Service
- Phase 2 (Core Business):
  - Product Service
  - Drop Service
  - Reservation Service
- Phase 3 (Supporting):
  - Notification Service
  - Payment Service
- Phase 4 (Advanced & Edge):
  - Analytics Service
  - API Gateway

## Current Status (quick)
- [ ] User Service — Security/JWT, controllers, tests
- [x] Store Service — CRUD skeleton + Liquibase with rollbacks
- [x] Docs updated — Maven build, CI, and checklists

See also: docs/phase-1-backend-checklist.md
