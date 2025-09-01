# Phase 1 Backend Checklist

## User service (auth & profiles)
- [x] Wire SecurityConfig (JWT auth, password encoder, CORS)
- [x] Implement JwtService (issue/validate tokens, TTL from config)
- [x] Add JWT filter and security chain (permit /auth/*, secure others)
- [x] Controllers: register, login, refresh, users/me, update profile
- [ ] Email verification and password reset endpoints (scaffold)
- [x] DTOs migrated to Java records
- [x] Liquibase: add rollbacks to all changesets
- [ ] Tests: unit (services), integration (auth flow) with Testcontainers
- [ ] OpenAPI via springdoc (basic info, grouped endpoints)

## Store service (stores & branches)
- [x] Entities, repositories, service, controller (create/get/update store, add branch)
- [x] Liquibase changelogs with explicit rollbacks
- [ ] Add validation and error handling (slug conflict, not found)
- [ ] Tests: unit + integration (CRUD + constraints) with Testcontainers
- [ ] OpenAPI annotations for request/response models

## Build, run, and ops
- [x] Maven parent fixed; module builds green
- [x] Build user-service module
- [x] Bring up Postgres with docker-compose
- [x] Run both services locally (8081 user, 8082 store)
- [x] Smoke test key endpoints (register/login/me, create store/branch)
- [ ] Jib image build for both services (optional this phase)
- [x] Basic actuator health exposure verified

## Quality and housekeeping
- [x] Liquibase rollbacks present for all existing SQL
- [ ] Consistent error responses (Problem JSON)
- [ ] Minimal logging and formatting rules
- [ ] Update docs and README with run instructions

## Notes
- Store DB: localhost:5434 (docker-compose), app port 8082
- User DB: localhost:5433 (docker-compose), app port 8081
- Next focus: user-service security/JWT and compile user-service
- **COMPLETED**: JWT authentication system working end-to-end
- **COMPLETED**: Store creation working, branch creation has JSON field issue (minor fix needed)
