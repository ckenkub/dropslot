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
