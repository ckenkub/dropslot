Security runbook — dropslot user-service

This runbook lists immediate operational steps to secure JWT and CORS configuration for the `user-service`.

Environment variables
- SECURITY_JWT_SECRET: base64-encoded HMAC secret used for signing JWTs
- SECURITY_JWT_TTL_SECONDS: access token TTL in seconds (default 3600)
- SECURITY_JWT_REFRESH_TTL_SECONDS: refresh token TTL in seconds (default 604800)
- SECURITY_CORS_ALLOWED_ORIGINS: comma-separated list of allowed origins for CORS (default `*` for dev)

Set environment variables (example on Linux):

```bash
export SECURITY_JWT_SECRET=$(openssl rand -base64 48)
export SECURITY_JWT_TTL_SECONDS=3600
export SECURITY_JWT_REFRESH_TTL_SECONDS=604800
export SECURITY_CORS_ALLOWED_ORIGINS=https://app.example.com,https://admin.example.com
```

Key rotation
1. Generate a new secret and store it in your secrets manager or environment.
2. Deploy services with a short overlap if you support multiple keys (not implemented).
3. Revoke old tokens by introducing server-side refresh token persistence and a revocation list.

Quick verification
- After starting the service, verify `/actuator/health` is UP and test login/refresh flows.

Notes
- For production, use a secrets manager (Vault, AWS Secrets Manager, etc.) and do not store secrets in code or plain files.
- Consider migrating to asymmetric signing (RS256) for cross-service validation without sharing symmetric secrets.
