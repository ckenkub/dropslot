# Phase 1 Backend Checklist

## User service (auth & profiles)
- [x] Wire SecurityConfig (JWT auth, password encoder, CORS)
- [x] Implement JwtService (issue/validate tokens, TTL from config)
- [x] Add JWT filter and security chain (permit /auth/*, secure others)
- [x] Controllers: register, login, refresh, users/me, update profile
- [x] Email verification and password reset endpoints (implemented)
- [x] DTOs migrated to Java records
- [x] Liquibase: add rollbacks to all changesets
- [x] Tests: unit (services), integration (auth flow) with Testcontainers
- [x] OpenAPI via springdoc (basic info, grouped endpoints)

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
	- Note: Jib is configured in each module's pom. Use `mvn -f backend/<module> -DskipTests jib:dockerBuild` to build to the local Docker daemon
	- [x] Local Jib build verified for `user-service` (image: `dropslot/user-service:latest`)
	- [x] CI jobs added (optional `jib:dockerBuild` job in `user-service-ci.yml`, `store-service-ci.yml` created)
- [x] Basic actuator health exposure verified

## Quality and housekeeping
- [x] Liquibase rollbacks present for all existing SQL
- [x] Consistent error responses (Problem JSON)
- [x] Minimal logging and formatting rules (Spotless, EditorConfig)
- [x] Logging policy: structured JSON, MDC fields, PII/masking documented (`docs/formatting-and-logging.md`)
- [x] Spotless: repository enforces formatting; developers must run `mvn -f backend spotless:apply` before commit
- [ ] Update docs and README with run instructions

### Recent infra & logging updates
- [x] Local Logstash test pipeline added (`backend/logstash/pipeline/logstash.conf`) and compose service (`logstash`) added to `backend/docker-compose.yml`.
- [x] `logback-spring.xml` updated to support `LOGSTASH_DEST` and profile-based console fallback for `docker` profile.

## PR / workflow
- [x] Create feature branch for cross-cutting changes (logging/formatting)
- [x] Open PR with description, checklist, and link to design/docs
- [x] Ensure CI runs `spotless:check` (reusable CI template includes spotless) — enforce branch protection separately

## Notes
- Store DB: localhost:5434 (docker-compose), app port 8082
- User DB: localhost:5433 (docker-compose), app port 8081
- Next focus: user-service security/JWT and compile user-service
- **COMPLETED**: JWT authentication system working end-to-end
- **COMPLETED**: Store creation working, branch creation has JSON field issue (minor fix needed)
