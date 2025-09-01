#!/usr/bin/env bash
set -euo pipefail

# Simple smoke test for store-service
# Checks that the service responds on port 8082 and returns a healthy status from actuator if available.
AUTH_URL=${AUTH_URL:-http://localhost:8082}

echo "Store-service smoke test: check port and /actuator/health or /"

# Wait for port to be open
for i in {1..60}; do
  if nc -z "localhost" 8082; then
    echo "port 8082 is open"
    break
  fi
  sleep 1
done

# Try actuator health first, fallback to root
if curl -sS --fail "$AUTH_URL/actuator/health" >/dev/null 2>&1; then
  echo "/actuator/health is available"
  curl -sS "$AUTH_URL/actuator/health" | jq . || true
else
  echo "/actuator/health not available, checking root path"
  curl -sS --fail "$AUTH_URL/" >/dev/null 2>&1 && echo "root path responded" || { echo "store-service did not respond as expected" >&2; exit 2; }
fi

echo "Store-service smoke test passed"
exit 0
