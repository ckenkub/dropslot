Formatting and logging conventions

Formatting
- Java: use Spotless with google-java-format (configured in `backend/pom.xml`).
- EditorConfig: a repository `.editorconfig` sets LF, UTF-8, 2-space indents for Java, and trims trailing whitespace for most files.
- Run:
  - Apply formatting locally: `mvn -f backend spotless:apply`
  - Check formatting: `mvn -f backend spotless:check`

Logging
- Use SLF4J API (the project uses Spring Boot logging configuration).
- Prefer structured log messages where helpful, but keep log levels conservative (DEBUG for developer info, INFO for important runtime events, WARN for recoverable issues, ERROR for failures).
- Avoid logging sensitive data (passwords, tokens, PII).

Structured JSON logs
- All services should emit structured JSON logs to make downstream ingestion easier (ELK/Loki/hosted). Use a JSON encoder for Logback or log4j2 and include these MDC fields where available:
  - requestId — correlation id for the HTTP request
  - userId — authenticated user id (if available)
  - service — service name (`user-service`, `store-service`, ...)
  - env — runtime environment (`local`, `ci`, `staging`, `prod`)

Example Logback configuration (add to `src/main/resources/logback-spring.xml`):

```xml
<!-- Minimal JSON encoder example using logstash-logback-encoder -->
<configuration>
  <include resource="org/springframework/boot/logging/logback/base.xml"/>
  <appender name="JSON" class="net.logstash.logback.appender.LoggingEventCompositeJsonEncoder">
    <!-- encoder configuration goes here; see logstash-logback-encoder docs -->
  </appender>
  <root level="INFO">
    <appender-ref ref="JSON" />
  </root>
</configuration>
```

MDC wiring
- Ensure a correlation id is generated for incoming HTTP requests and placed into MDC (e.g., `requestId`). Propagate this id across service calls and background tasks so logs can be correlated.

Observability
- Detailed log pipelines, retention, and dashboards will be planned when we apply observability (metrics/log ingest). For now, ensure logs are JSON and include the MDC fields above.

Commit
- Run `mvn -f backend spotless:check` in CI before merge. If formatting fails, run `mvn -f backend spotless:apply` locally, verify, commit and push.

## What to log vs redact

Always assume logs are shared, searchable, and long-lived. Log only what you need to operate and debug; redact or omit sensitive data.

Do log (safe-by-default):
- Event names and outcomes: "login successful", "verification failed (invalid code)".
- Stable identifiers that are not secrets: userId, requestId, jti (token ID), role codes.
  - Prefer putting userId/requestId into MDC and avoid repeating them in the message text.
- Non-PII metadata: counts, durations, sizes, HTTP status codes, feature flags (names only).
- Masked email addresses when needed for correlation (use `LogUtils.maskEmail`).

Don’t log (ever):
- Passwords, raw tokens (JWT/refresh), API keys, secrets, TOTP/verification codes.
- Full email addresses or other PII unless masked/anonymized.
- Authorization headers, cookies, or session identifiers.
- Full request/response bodies except for explicitly whitelisted fields in DEBUG-only diagnostics.

Sometimes log, but with caution/anonymization:
- IP addresses and user-agents: only when needed for security auditing; consider truncation/anonymization.
- Database record contents: log identifiers or counts instead of full payloads.

MDC and duplication:
- Set `requestId` at the edge (HTTP filter). Set `userId` in MDC once the identity is known and clear it in a `finally` block.
- When `userId` is in MDC, avoid repeating it in the message. Prefer messages like "Update profile" instead of "Update profile for userId=...".

Examples
- Good: `log.info("Login successful jti={}", jti);` (userId carried via MDC)
- Good: `log.info("Password reset requested for email={}", LogUtils.maskEmail(email));`
- Bad: `log.info("Login successful token={}", rawJwt);` (logs a secret)
- Bad: `log.info("Verify email code={} email={}", code, email);` (logs code and PII)

Pre-commit formatting (Spotless)
- Always run Spotless before committing Java changes:
  - Apply formatting: `mvn -f backend spotless:apply`
  - Verify formatting: `mvn -f backend spotless:check`
- Optional: set up a local pre-commit hook to enforce this (create `.git/hooks/pre-commit`):

```bash
#!/usr/bin/env bash
set -euo pipefail
mvn -q -f backend spotless:apply
mvn -q -f backend spotless:check
```

Make the hook executable: `chmod +x .git/hooks/pre-commit`.
