## CI & Publish workflows

This page describes the GitHub Actions workflows in this repository, how they are triggered, the secrets they require, and quick verification steps for the GHCR publish flow.

### Workflows (location: `.github/workflows`)

- `publish-ghcr.yml` — Publish images to GHCR
  - Triggers: `push` of tags matching `v*` and `workflow_dispatch` (manual)
  - Purpose: build `user-service` and `store-service` with Maven + Jib and push images to `ghcr.io/<owner>/<service>:<tag>`
  - Key notes: Docker login uses `GHCR_USERNAME` + `GHCR_TOKEN`; workflow sets `permissions: packages: write`.

- `user-service-ci.yml` — CI for `backend/user-service`
  - Triggers: `pull_request` that touches `backend/user-service/**`, and `workflow_dispatch`.
  - Purpose: calls the reusable Java CI workflow, optionally runs `jib:dockerBuild` when manually dispatched or on `main`.

- `store-service-ci.yml` — CI for `backend/store-service`
  - Triggers and purpose: same pattern as `user-service-ci.yml` but for the `store-service` module.

- `ci-e2e.yml` — End-to-end smoke tests
  - Triggers: `push` to `main` (backend paths) and `workflow_dispatch`.
  - Purpose: runs docker-compose based smoke tests by calling the reusable workflow with `run_e2e: true`.

- `ci-java-reusable.yml` — Reusable Java CI
  - Trigger: `workflow_call` (inputs include `module_path`, `run_integration_tests`, `run_e2e`, `jdk-version`).
  - Purpose: centralized Spotless format check, unit/integration tests, and optional e2e smoke job that uploads logs as artifacts.

### Secrets & permissions

- `GHCR_TOKEN` — Personal access token used to authenticate and push to GHCR. Recommended scopes for reliability: `write:packages`, `read:packages`, and `repo` if the repo is private. Use a classic PAT if you need `packages:write`.
- `GHCR_USERNAME` — Username for `docker login` and Jib auth.
- `GITHUB_TOKEN` — Available in workflows; used as a fallback in `publish-ghcr.yml` but may not have `packages:write` in all orgs.

Keep secrets scoped to the repository and never print tokens in plaintext in logs.

### How to verify a tag publish succeeded (quick)

1. Push a tag (example):

```bash
TAG=vtest-$(date +%s)
git tag -a "$TAG" -m "ci: manual publish $TAG"
git push origin "refs/tags/$TAG"
```

2. Open GitHub → Actions → select "Publish images to GHCR" → open the run for the tag. Look for lines like:

```
Built and pushed image as ghcr.io/<OWNER>/user-service:<TAG>
BUILD SUCCESS
```

3. Confirm images in GHCR (UI):
   - Your profile → Packages (filter container) or repository → Packages (may be under the repo nav or More). Find `user-service` / `store-service` and check the versions list for the tag.

4. (Optional CLI pull) If package is private, authenticate and pull:

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GHCR_USERNAME" --password-stdin
docker pull ghcr.io/<OWNER>/user-service:<TAG>
```

### Common failures & troubleshooting

- 403 Forbidden during push: PAT lacks `write:packages` or the PAT owner lacks permission to publish to the target owner/org. Fix: create a classic PAT with `write:packages` and set it as `GHCR_TOKEN`, and ensure `GHCR_USERNAME` matches.
- GH API returns 403/404 when listing packages: token lacks `read:packages`, or you're querying the wrong owner endpoint (user vs repo vs org). Use a token with `read:packages` to list programmatically.
- Jib build failures: usually caused by compilation or missing classes. Ensure Maven `clean package` succeeds before `jib:build`.
- Spotless failures: run `mvn spotless:apply` or fix formatting locally and re-run CI.

### Quick CLI checks

- List recent publish runs:
```bash
gh run list --repo <owner>/<repo> --workflow=publish-ghcr.yml --limit 5
```
- View logs for a run:
```bash
gh run view <RUN_ID> --repo <owner>/<repo> --log
# search for "Built and pushed image as ghcr.io"
```
- List container packages (requires token with `read:packages`):
```bash
gh api "/user/packages?package_type=container"
gh api "/repos/<owner>/<repo>/packages?package_type=container"
```

### Recommended small improvements

- Add an end-of-workflow verification step to `publish-ghcr.yml` that calls the Packages API to confirm the expected package version exists, and uploads the JSON as an artifact.
- Emit a small JSON artifact containing image digests and tags after successful publish for traceability.

---

This file is a short reference. For anything you'd like expanded (example log snippets, adding the verification step, or turning these notes into a `docs/` README section), tell me which item and I will implement it.
