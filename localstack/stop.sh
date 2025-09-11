#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"
COMPOSE=${COMPOSE:-docker compose}
$COMPOSE down -v

echo "LocalStack stopped and data removed."
