#!/usr/bin/env bash
set -euo pipefail

wait_for() {
  local url="$1" name="$2" retries=60
  echo "Waiting for $name at $url";
  for i in $(seq 1 $retries); do
    if curl -fsS -m 3 "$url" >/dev/null 2>&1; then echo "$name is up"; return 0; fi
    sleep 2
  done
  echo "Timed out waiting for $name"; return 1
}

wait_for http://localhost:8080/actuator/health "gateway"
wait_for http://localhost:8081/actuator/health "user-service"
wait_for http://localhost:8082/actuator/health "store-service"
