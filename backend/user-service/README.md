# user-service

Short notes for developers about mailer configuration and switching between the in-memory test mailer and SMTP.

## Mailer implementations

- `com.dropslot.user.mail.Mailer` — interface used by application code (`AuthService` etc.).
- `com.dropslot.user.mail.InMemoryMailer` — test-friendly implementation that records sent messages (used in integration tests and CI by default).
- `com.dropslot.user.mail.SmtpMailer` — production-capable implementation that sends real email via Spring's `JavaMailSender`.

## Which mailer is active

- The application injects `Mailer` by type. Which concrete bean is registered is controlled by Spring configuration.
- `InMemoryMailer` is a normal component and will be available in local runs/tests.
- `SmtpMailer` is annotated with `@ConditionalOnProperty(name="app.mailer.type", havingValue="smtp")` and will only be registered when the property `app.mailer.type=smtp` is set.

## Enabling SMTP

To enable the SMTP mailer in a deployment or local run, set the property and provide mail server settings. Example `application.yml` snippet:

```yaml
app:
  mailer:
    type: smtp

spring:
  mail:
    host: smtp.example.com
    port: 587
    username: ${SMTP_USERNAME}
    password: ${SMTP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
```

Prefer providing credentials via environment variables or a secrets manager (`SMTP_USERNAME`, `SMTP_PASSWORD`) rather than committing them to the repo.

## Tests and CI

- CI and local integration tests should keep using `InMemoryMailer` (no external SMTP). The `AuthFlowIntegrationTest` reads the in-memory mailbox to extract verification/reset tokens.

## Next steps / recommendations

- If you want a convenience profile for local SMTP testing, add an `application-local-smtp.yml` with sanitized defaults and document how to run it.
- Optionally add a small README section for how to rotate or revoke SMTP credentials used by deployments.
