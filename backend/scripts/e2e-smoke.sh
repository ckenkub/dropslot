#!/usr/bin/env bash
set -euo pipefail

export GATEWAY_BASE=${GATEWAY_BASE:-http://localhost:8080}
# Use deterministic email per run
export DS_EMAIL=${DS_EMAIL:-"ci-user+$(date +%s)@example.com"}
export DS_PASSWORD=${DS_PASSWORD:-"Passw0rd!"}
export USE_K8S_LOGS=0

chmod +x "$(dirname "$0")/test-gateway.sh"
"$(dirname "$0")/test-gateway.sh"
