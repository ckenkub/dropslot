#!/usr/bin/env bash
set -euo pipefail

# Wait for a TCP port to be open on a container (via localhost:port)
# Usage: ci-wait-for.sh <host> <port> <timeout_seconds>
HOST=${1:-localhost}
PORT=${2:-5433}
TIMEOUT=${3:-60}

echo "Waiting for $HOST:$PORT (timeout ${TIMEOUT}s)"
end=$((SECONDS+TIMEOUT))
while [ $SECONDS -lt $end ]; do
  if nc -z "$HOST" "$PORT" 2>/dev/null; then
    echo "$HOST:$PORT is up"
    exit 0
  fi
  sleep 1
done

echo "Timeout waiting for $HOST:$PORT" >&2
exit 1
