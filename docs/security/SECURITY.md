Security Overview — dropslot backend

This document summarizes the security design and the concrete security code we implemented for Phase 1 (user-service). It lists components, flows, configuration keys, endpoints, examples, risks and recommended next steps.

Goals
- Provide stateless JWT-based authentication for REST APIs
- Use strong password hashing for credentials
- Enforce role-based authorization in APIs
- Provide access + refresh token flow
- Keep configuration explicit and easy to change (constructor-injection)

Scope
- Current implementation covers `user-service` only. Files referenced are under `backend/user-service`.

Quick index
- Components
- Configuration
- Token flows (registration, login, refresh, protected request)
- curl examples
- Risks / mitigations
- Recommended next steps
- Mapping to source files

## Components

1) JwtService
- Location: `backend/user-service/src/main/java/com/dropslot/user/security/JwtService.java`
- Responsibility: generate access tokens and refresh tokens, validate tokens, extract subject and roles, check expiration.
- Implementation notes:
  - Constructor-injected values: `security.jwt.secret` (Base64), `security.jwt.ttlSeconds`, `security.jwt.refreshTtlSeconds`.
  - Access tokens include custom claim `roles` (List<String>).
  - Refresh tokens are JWTs with longer TTL and no roles claim (only subject).
  - Public methods: `generate(subject, claims)`, `generateRefreshToken(subject)`, `isTokenValid(token)`, `extractSubject(token)`, `extractRoles(token)`, `getTtlSeconds()`.

2) JwtAuthenticationFilter
- Location: `backend/user-service/src/main/java/com/dropslot/user/security/JwtAuthenticationFilter.java`
- Responsibility: intercept requests, parse `Authorization: Bearer <token>`, validate access token using `JwtService`, build `Authentication` with authorities derived from roles claim and set SecurityContext.
- Behavior: silently ignores invalid tokens (request proceeds unauthenticated).

3) SecurityConfig
- Location: `backend/user-service/src/main/java/com/dropslot/user/config/SecurityConfig.java`
- Responsibility: configure Spring Security: disable CSRF, configure CORS, stateless session, permit public endpoints (`/auth/**`, `/actuator/**`, openapi), require auth for others, wire `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`, configure `DaoAuthenticationProvider` with `CustomUserDetailsService`.

4) CustomUserDetailsService
- Location: `backend/user-service/src/main/java/com/dropslot/user/service/CustomUserDetailsService.java`
- Responsibility: load user by email from DB, map domain roles to Spring `GrantedAuthority` values (prefixed with `ROLE_`). Used by the authentication provider.

5) Password encoding
- Implementation: `BCryptPasswordEncoder` bean in `SecurityConfig`.
- Notes: BCrypt provides per-password salt and adjustable work factor.

6) AuthService / AuthController
- Location: `backend/user-service/src/main/java/com/dropslot/user/service/AuthService.java` and `.../api/AuthController.java`
- Responsibility: registration, login, refresh
  - `register` stores user with password hashed (BCrypt) and assigns `CUSTOMER` role.
  - `login` validates password, issues `accessToken` and `refreshToken` (both JWTs). Returns `AuthDtos.TokenResponse` with both tokens, `tokenType` and `expiresIn`.
  - `refreshAccessToken(refreshToken)` validates refresh token, loads user by id, issues new access token and rotates refresh token (returns a new refresh token). This is stateless validation of refresh token.

## Configuration keys
- `security.jwt.secret` (Base64-encoded secret used for HMAC signing)
- `security.jwt.ttlSeconds` (access token TTL in seconds)
- `security.jwt.refreshTtlSeconds` (refresh token TTL in seconds)
- `server.port` (service port)

Example `application.yml` snippet (currently used)

```
security:
  jwt:
    secret: <BASE64_SECRET>
    ttlSeconds: 3600
    refreshTtlSeconds: 604800
```

Important: Move `security.jwt.secret` to environment variables or secret store in production. Do not commit long-lived secret material to VCS.

## Token flows

1) Registration (no tokens issued)
- POST /auth/register
- Body: `{ "email": "user@example.com", "password": "password123", "name": "Name" }`
- Result: user created in DB

2) Login (get access + refresh tokens)
- POST /auth/login
- Body: `{ "email": "user@example.com", "password": "password123" }`
- Result: JSON
  ```json
  {
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
  ```
- Access token: short TTL (configured via `ttlSeconds`), contains `roles` claim.
- Refresh token: longer TTL, used to obtain new access token.

3) Refresh
- POST /auth/refresh
- Body: `{ "refreshToken": "<refreshJwt>" }`
- Server validations:
  - `JwtService.isTokenValid(refreshToken)` ensures signature and expiration
  - `JwtService.extractSubject(refreshToken)` yields user id; loaded from DB
- Result: new access token (and rotated new refresh token) returned.

4) Protected request
- Send header: `Authorization: Bearer <accessToken>`
- `JwtAuthenticationFilter` will set `SecurityContext` with subject as principal and authorities from roles.

## curl examples

Register

```bash
curl -X POST -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","password":"password123","name":"Me"}' \
  http://localhost:8081/auth/register
```

Login

```bash
curl -X POST -H 'Content-Type: application/json' \
  -d '{"email":"me@example.com","password":"password123"}' \
  http://localhost:8081/auth/login | jq .
```

Refresh

```bash
curl -X POST -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<token>"}' \
  http://localhost:8081/auth/refresh | jq .
```

Protected endpoint example

```bash
curl -H "Authorization: Bearer <accessToken>" http://localhost:8081/users/me
```

## Security considerations (risks and mitigations)

1) Secret management
- Risk: `security.jwt.secret` stored in repo or plaintext config
- Mitigation: Load from environment variables, vault, or container secret files. Rotate regularly.

2) Stateless refresh tokens
- Risk: If a refresh token is stolen, it can be used until expiry
- Mitigation (recommended): Persist refresh tokens (or identifiers) server-side and maintain revocation list. On refresh rotate and revoke old token. Detect reuse and revoke all sessions for that user.

3) Token revocation and logout
- Current: No server-side revocation. Implement token storage to support logout and revocation.

4) Refresh token reuse detection
- Store fingerprint or token ID. If same token used twice (after rotation), reject and require re-authentication.

5) CSRF
- API is stateless and uses Authorization header; CSRF risk is low if tokens are stored in memory or secure httpOnly cookies. If storing tokens in cookies, add CSRF protections.

6) Password policies
- Enforce strong password policies, rate-limit login attempts, and consider account lockout on repeated failures.

7) Rate limiting
- Protect `/auth/login` and `/auth/refresh` endpoints with rate limiting (API gateway, nginx, or middleware).

8) CORS
- Currently configured permissive CORS (allowed origin patterns `*`) — restrict this to the frontend domains in production.

9) Logging and monitoring
- Ensure auth failures and token refresh events are logged with limited PII. Monitor for spikes in token refresh usage.

10) Key rotation
- Plan for key rotation. Use `kid` headers and multiple keys if needed.

11) Algorithm choices
- Current: HMAC-SHA256 (HS256) with a secret. Consider asymmetric keys (RS256) if you need public verification without secret sharing.

## Recommended next steps (prioritized)

1. Move `security.jwt.secret` out of VCS into env or secret manager. Use unique secret per environment.
2. Implement persistent refresh tokens with rotation and revocation (server-side storage).
3. Add unit/integration tests for `JwtService` and `AuthService` refresh flow.
4. Harden CORS and add rate limiting to auth endpoints.
5. Implement refresh token reuse detection and session management.
6. Add OpenAPI security schemes and document authentication flows in API docs.
7. Consider switching to asymmetric signing (RS256) for cross-service token validation if services are deployed separately.

## Mapping to source files (quick)
- `JwtService` — `backend/user-service/src/main/java/com/dropslot/user/security/JwtService.java`
- `JwtAuthenticationFilter` — `backend/user-service/src/main/java/com/dropslot/user/security/JwtAuthenticationFilter.java`
- `SecurityConfig` — `backend/user-service/src/main/java/com/dropslot/user/config/SecurityConfig.java`
- `CustomUserDetailsService` — `backend/user-service/src/main/java/com/dropslot/user/service/CustomUserDetailsService.java`
- `AuthService` — `backend/user-service/src/main/java/com/dropslot/user/service/AuthService.java`
- `AuthController` — `backend/user-service/src/main/java/com/dropslot/user/api/AuthController.java`
- DTOs — `backend/user-service/src/main/java/com/dropslot/user/api/dto/AuthDtos.java`

## Tests you can/should add
- `JwtServiceTest` — generate -> validate -> extract claims, expired token test
- `AuthServiceTest` — login flow returns both tokens, refresh flow rotates refresh token
- `AuthControllerIT` — end-to-end register/login/refresh + protected endpoint access

## How to run the smoke tests (example)

```bash
# build
cd backend/user-service
mvn -DskipTests clean package

# start
cd target
nohup java -jar user-service-0.1.0-SNAPSHOT.jar > /tmp/user-service.log 2>&1 &

# register/login/refresh
curl -X POST -H 'Content-Type: application/json' -d '{"email":"me@example.com","password":"password123","name":"Me"}' http://localhost:8081/auth/register

LOGIN_JSON=$(curl -s -X POST -H 'Content-Type: application/json' -d '{"email":"me@example.com","password":"password123"}' http://localhost:8081/auth/login)
ACCESS=$(echo "$LOGIN_JSON" | jq -r .accessToken)
REFRESH=$(echo "$LOGIN_JSON" | jq -r .refreshToken)

curl -H "Authorization: Bearer $ACCESS" http://localhost:8081/users/me

curl -s -X POST -H 'Content-Type: application/json' -d '{"refreshToken":"'$REFRESH'"}' http://localhost:8081/auth/refresh | jq .
```

## Contact / Ownership
- Author: implementation by development work in repo
- For changes: edit the listed source files or open an issue in the project tracker


---

If you'd like, I can:
- Add a short `docs/security/checklist.md` with step-by-step migration tasks (e.g. move secret to env, add DB for refresh tokens), or
- Implement server-side refresh token persistence now (create DB table, repository, update `AuthService` to persist and revoke tokens).

Which do you want next?
