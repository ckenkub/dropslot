#!/usr/bin/env bash

# DropSlot API Gateway smoke test
# Hits the same flows via the API Gateway routes:
# - POST /api/auth/register -> 201/409
# - POST /api/auth/login -> if not active, send /api/auth/verify/send, POST /api/auth/verify, retry login
# - GET  /api/users/me (bearer)
# - POST /api/stores -> GET /api/stores/{id} -> POST /api/stores/{id}/branches
# - Health: /actuator/health (gateway)
#
# Env overrides:
#   GATEWAY_BASE (default: http://localhost:8080)
#   DS_EMAIL (default: tester.<epoch>@example.com)
#   DS_PASSWORD (default: Passw0rd!)
#   CURL_OPTS (extra curl flags)
 #   USE_K8S_LOGS (default: 1) if kubectl present, try to scrape verification token from user-service logs

set -u

GATEWAY_BASE=${GATEWAY_BASE:-http://localhost:8080}
DS_EMAIL=${DS_EMAIL:-"tester.$(date +%s)@example.com"}
DS_PASSWORD=${DS_PASSWORD:-"Passw0rd!"}
CURL_OPTS=${CURL_OPTS:-}
USE_K8S_LOGS=${USE_K8S_LOGS:-1}

have_jq() { command -v jq >/dev/null 2>&1; }

color() { local c="$1"; shift; printf "\033[%sm%s\033[0m" "$c" "$*"; }
info() { echo "$(color 36 [INFO]) $*"; }
ok() { echo "$(color 32 [ OK ]) $*"; }
warn() { echo "$(color 33 [WARN]) $*"; }
err() { echo "$(color 31 [ERR ]) $*"; }

pretty_json() {
  if have_jq; then jq -C . 2>/dev/null || cat; else cat; fi
}

# Try to extract verification token for DS_EMAIL from k8s logs (InMemoryMailer prints token=XXXXXXXX)
get_token_from_k8s_logs() {
  command -v kubectl >/dev/null 2>&1 || return 1
  local pod
  pod=$(kubectl get pods -l app=user-service -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)
  [[ -z "$pod" ]] && return 1
  kubectl logs "$pod" --since=10m 2>/dev/null | grep InMemoryMailer | grep -F "${DS_EMAIL}" | tail -1 | sed -n 's/.*token=\([A-Fa-f0-9]\{8\}\).*/\1/p'
}

# send METHOD URL [DATA_JSON] [AUTH]
send() {
  local method="$1" url="$2" data="${3:-}" auth="${4:-}"
  local headers=("-H" "Content-Type: application/json" "-H" "Accept: application/json")
  [[ "${auth}" == "auth" && -n "${ACCESS_TOKEN:-}" ]] && headers+=("-H" "Authorization: Bearer ${ACCESS_TOKEN}")

  local response http_code body
  response=$(curl -sS ${CURL_OPTS} -w "HTTPSTATUS:%{http_code}" -X "$method" "${headers[@]}" ${data:+-d "$data"} "$url") || response="HTTPSTATUS:000"
  http_code=${response##*HTTPSTATUS:}
  body=${response%HTTPSTATUS:*}

  echo "$body" | pretty_json
  echo "-- HTTP $http_code"
  echo "$http_code"
}

step() { echo; info "$*"; }

ACCESS_TOKEN=""

step "Gateway health check"
echo "> GET ${GATEWAY_BASE}/actuator/health"
send GET "${GATEWAY_BASE}/actuator/health" >/dev/null

API=${GATEWAY_BASE}/api

step "Register via gateway (may 409 if already exists)"
REGISTER_PAYLOAD=$(cat <<JSON
{ "email": "${DS_EMAIL}", "password": "${DS_PASSWORD}", "name": "Test User", "acceptTerms": true }
JSON
)
echo "> POST ${API}/auth/register"
code=$(send POST "${API}/auth/register" "$REGISTER_PAYLOAD")
if [[ "$code" == "201" ]]; then ok "Registered ${DS_EMAIL}"; else warn "Register status: $code (expected 201/409)"; fi

step "Login via gateway"
LOGIN_PAYLOAD=$(cat <<JSON
{ "email": "${DS_EMAIL}", "password": "${DS_PASSWORD}" }
JSON
)
echo "> POST ${API}/auth/login"
resp=$(curl -sS ${CURL_OPTS} -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -H "Accept: application/json" -d "$LOGIN_PAYLOAD" "${API}/auth/login")
http_code=${resp##*HTTPSTATUS:}
body=${resp%HTTPSTATUS:*}
echo "$body" | pretty_json
if [[ "$http_code" == "200" ]]; then
  ACCESS_TOKEN=$(echo "$body" | (have_jq && jq -r '.accessToken // .data.accessToken // empty' || sed -n 's/.*"accessToken"\s*:\s*"\([^"]*\)".*/\1/p'))
else
  ACCESS_TOKEN=""
fi
if [[ -z "$ACCESS_TOKEN" ]]; then
  warn "Login failed (HTTP $http_code). Will attempt email verification and retry."
  step "Send verification code via gateway"
  echo "> POST ${API}/auth/verify/send"
  send POST "${API}/auth/verify/send" "{ \"email\": \"${DS_EMAIL}\" }" >/dev/null

  token=""
  if [[ "$USE_K8S_LOGS" == "1" ]]; then
    step "Extract verification token from k8s logs"
    token=$(get_token_from_k8s_logs || true)
  fi
  if [[ -z "$token" ]]; then
    warn "Could not auto-extract verification token. If running locally, check user-service logs for InMemoryMailer output and set DS_VERIFY_CODE env var."
    token=${DS_VERIFY_CODE:-}
  fi
  if [[ -z "$token" ]]; then
    err "No verification token available. Skipping verify step."
  else
    step "Verify email via gateway"
    VERIFY_PAYLOAD=$(cat <<JSON
{ "email": "${DS_EMAIL}", "code": "${token}" }
JSON
)
    echo "> POST ${API}/auth/verify"
    send POST "${API}/auth/verify" "$VERIFY_PAYLOAD" >/dev/null
    step "Retry login via gateway"
    resp=$(curl -sS ${CURL_OPTS} -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -H "Accept: application/json" -d "$LOGIN_PAYLOAD" "${API}/auth/login")
    http_code=${resp##*HTTPSTATUS:}
    body=${resp%HTTPSTATUS:*}
    echo "$body" | pretty_json
    ACCESS_TOKEN=$(echo "$body" | (have_jq && jq -r '.accessToken // .data.accessToken // empty' || sed -n 's/.*"accessToken"\s*:\s*"\([^"]*\)".*/\1/p'))
  fi
fi
if [[ -n "$ACCESS_TOKEN" ]]; then ok "Got access token"; else err "Failed to obtain access token"; fi

step "Get current user (/users/me) via gateway"
echo "> GET ${API}/users/me"
send GET "${API}/users/me" "" auth >/dev/null

step "Create a store via gateway"
STORE_SLUG="demo-$(date +%s)"
CREATE_STORE_PAYLOAD=$(cat <<JSON
{ "name": "Demo Store", "slug": "${STORE_SLUG}", "tenantKey": "TEN_${STORE_SLUG}", "logoUrl": "https://example.com/logo.png" }
JSON
)
echo "> POST ${API}/stores"
create_resp=$(curl -sS ${CURL_OPTS} -w "HTTPSTATUS:%{http_code}" -X POST -H "Content-Type: application/json" -H "Accept: application/json" -H "Authorization: Bearer ${ACCESS_TOKEN}" -d "$CREATE_STORE_PAYLOAD" "${API}/stores")
create_code=${create_resp##*HTTPSTATUS:}
create_body=${create_resp%HTTPSTATUS:*}
echo "$create_body" | pretty_json
store_id=$(echo "$create_body" | (have_jq && jq -r '.id // empty' || sed -n 's/.*"id"\s*:\s*"\([^"]*\)".*/\1/p'))
if [[ -n "$store_id" ]]; then ok "Created store id=$store_id"; else warn "Create store status: $create_code"; fi

if [[ -n "$store_id" ]]; then
  step "Get store by id via gateway"
  echo "> GET ${API}/stores/${store_id}"
  send GET "${API}/stores/${store_id}" >/dev/null

  step "Add branch via gateway"
  ADD_BRANCH_PAYLOAD='{"name":"Main Branch","address":"123 Main","lat":40.0,"lng":-74.0,"phone":"+123456789"}'
  echo "> POST ${API}/stores/${store_id}/branches"
  send POST "${API}/stores/${store_id}/branches" "$ADD_BRANCH_PAYLOAD" >/dev/null
fi

echo
ok "Gateway tests completed."
