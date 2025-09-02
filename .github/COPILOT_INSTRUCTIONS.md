Repository conventions (for Copilot / contributors)

We follow the same development pattern for all services in this repository: use the established conventions for Liquibase changelogs, DTOs, error handling, OpenAPI annotations, testing, and CI; new services should mirror the structure and practices already present in `user-service` and `store-service`.

Liquibase changelogs
- Use the SQL-based changelog format used in existing files (e.g. `01-create-core-tables.sql`).
- Each SQL changelog must start with:
  --liquibase formatted sql
  --changeset <author>:<id>
  - Use the repository GitHub id as the changeset author. For this repo use `ckenkub` (example: `--changeset ckenkub:05`). Copilot and contributors should always use this author string.
- Include a single rollback statement at the end using `--rollback` followed by SQL to undo the changes.
- Filenames should be prefixed with their sequence number (01-, 02-, ...), use hyphen separators and `.sql` extension.
- Do not add XML changelogs unless there's a specific reason; the project uses SQL format for readability and easier reviews.

Code, DB, and testing
- For simple scaffolds (like email verification), prefer in-memory or DB-backed scaffolding that is safe for CI.
- If adding a new table/entity, add a matching SQL changelog in `backend/*/src/main/resources/db/changelog/` and a rollback.
- Keep changes small and testable (run `mvn -DskipTests package` to check compilation).

CI and smoke tests
- Smoke tests scripts live in `backend/scripts` and should be idempotent and safe to run in CI.
- Avoid external network dependencies in CI (e.g., real SMTP). Use logs or an in-memory/fake mailer for tests.

Pull requests
- Use feature branches and concise commit messages: `feat(...)`, `ci(...)`, `fix(...)`.
- Open PRs against `main` and ensure CI passes before merge.
