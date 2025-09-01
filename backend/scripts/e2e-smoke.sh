#!/usr/bin/env bash
set -euo pipefail

# Automated end-to-end smoke test for user-service auth flow
# - login -> refresh -> check refresh_tokens in Postgres container
# Config via env vars (defaults below)
AUTH_URL=${AUTH_URL:-http://localhost:8081}
EMAIL=${EMAIL:-test@example.com}
PASSWORD=${PASSWORD:-password123}
NAME=${NAME:-"Test User"}
DB_CONTAINER=${DB_CONTAINER:-ds-postgres-user}
DB_USER=${DB_USER:-user}
DB_NAME=${DB_NAME:-user_db}

echo "E2E smoke test: auth login -> refresh -> DB check"
echo "AUTH_URL=$AUTH_URL, EMAIL=$EMAIL, DB_CONTAINER=$DB_CONTAINER"

tmpfile=$(mktemp)
trap 'rm -f "$tmpfile"' EXIT

# Optionally clean existing refresh tokens for the test user to make the run deterministic
# Set CLEAN_REFRESH_TOKENS=false to skip this destructive step
CLEAN_REFRESH_TOKENS=${CLEAN_REFRESH_TOKENS:-true}
if [ "$CLEAN_REFRESH_TOKENS" = "true" ]; then
  echo -e "\n[0/4] Cleaning refresh_tokens for $EMAIL in $DB_CONTAINER"
  CLEAN_SQL="DELETE FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email='$EMAIL');"
  if docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "$CLEAN_SQL"; then
    echo "Cleaned refresh_tokens for $EMAIL"
  else
    echo "Warning: failed to clean refresh_tokens (continuing)" >&2
  fi
fi

# Ensure test user exists (idempotent)
echo -e "\n[0.5/4] Registering user $EMAIL (idempotent)"
REG_REQ=$(jq -n --arg e "$EMAIL" --arg p "$PASSWORD" --arg n "$NAME" '{email:$e,password:$p,name:$n}')
REG_RESP=$(curl -sS -w "\n%{http_code}" -X POST "$AUTH_URL/auth/register" -H 'Content-Type: application/json' -d "$REG_REQ" || true)
REG_HTTP=$(echo "$REG_RESP" | tail -n1)
REG_BODY=$(echo "$REG_RESP" | sed '$d' || true)
if [ -z "$REG_HTTP" ]; then
  echo "Register request failed to connect" >&2
  echo "$REG_BODY" || true
  exit 1
fi
if [ "$REG_HTTP" -eq 200 ] || echo "$REG_BODY" | grep -qi "already registered"; then
  echo "User exists or registered successfully (HTTP $REG_HTTP)"
else
  echo "Register failed (HTTP $REG_HTTP)" >&2
  echo "$REG_BODY" | jq . || echo "$REG_BODY"
  exit 1
fi

# Login
echo -e "\n[1/4] Login"
LOGIN_RESP=$(curl -sS -w "\n%{http_code}" -X POST "$AUTH_URL/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
HTTP=$(echo "$LOGIN_RESP" | tail -n1)
BODY=$(echo "$LOGIN_RESP" | sed '$d')

if [ "$HTTP" -ne 200 ]; then
  echo "Login failed (HTTP $HTTP)" >&2
  echo "$BODY" | jq . || echo "$BODY"
  exit 2
fi

echo "Login response:"; echo "$BODY" | jq .
ACCESS_TOKEN=$(echo "$BODY" | jq -r .accessToken)
REFRESH_TOKEN=$(echo "$BODY" | jq -r .refreshToken)

if [ -z "$REFRESH_TOKEN" ] || [ "$REFRESH_TOKEN" = "null" ]; then
  echo "No refresh token returned from login" >&2
  exit 3
fi

# Use refresh token
echo -e "\n[2/4] Refresh using returned refresh token"
REFRESH_RESP=$(curl -sS -w "\n%{http_code}" -X POST "$AUTH_URL/auth/refresh" -H 'Content-Type: application/json' -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
HTTP_R=$(echo "$REFRESH_RESP" | tail -n1)
BODY_R=$(echo "$REFRESH_RESP" | sed '$d')

if [ "$HTTP_R" -ne 200 ]; then
  echo "Refresh failed (HTTP $HTTP_R)" >&2
  echo "$BODY_R" | jq . || echo "$BODY_R"
  exit 4
fi

echo "Refresh response:"; echo "$BODY_R" | jq .
NEW_REFRESH_TOKEN=$(echo "$BODY_R" | jq -r .refreshToken)

# Query DB for user's refresh tokens
echo -e "\n[3/4] Query refresh_tokens for $EMAIL in $DB_CONTAINER"
SQL="SELECT id,jti,user_id,revoked,issued_at,expires_at,replaced_by_jti FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email='$EMAIL') ORDER BY issued_at DESC LIMIT 10;"
docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "$SQL" || { echo 'Failed to query DB' >&2; exit 5; }

# Count rows
COUNT_SQL="SELECT count(*) FROM refresh_tokens WHERE user_id IN (SELECT id FROM users WHERE email='$EMAIL');"
docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -c "$COUNT_SQL" || { echo 'Failed to count DB rows' >&2; exit 6; }

# Quick verification: ensure at least 1 row exists
COUNT=$(docker exec "$DB_CONTAINER" psql -U "$DB_USER" -d "$DB_NAME" -t -c "$COUNT_SQL" | tr -d '[:space:]')
if [ -z "$COUNT" ]; then
  echo "Could not determine refresh_tokens count" >&2
  exit 7
fi

if [ "$COUNT" -lt 1 ]; then
  echo "No refresh tokens found for $EMAIL" >&2
  exit 8
fi

echo -e "\n[4/4] Smoke test successful — refresh token persisted. Rows: $COUNT"

exit 0
