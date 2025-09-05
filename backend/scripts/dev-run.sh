#!/usr/bin/env bash
set -euo pipefail
# Dev helper: start postgres containers and run services on the host JVM
# Usage:
#  ./scripts/dev-run.sh start   -> starts postgres containers and services
#  ./scripts/dev-run.sh stop    -> stops services started by this script
#  ./scripts/dev-run.sh status  -> show service status and health endpoints

REPO_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$REPO_ROOT"

start_postgres() {
  echo "Starting postgres containers via docker-compose..."
  docker-compose up -d postgres-user postgres-store
}

wait_for_port() {
  local host=$1; local port=$2; local retries=20; local delay=1
  echo "Waiting for $host:$port..."
  for i in $(seq 1 $retries); do
    if nc -z "$host" "$port" >/dev/null 2>&1; then
      echo "$host:$port is available"
      return 0
    fi
    sleep $delay
  done
  echo "Timed out waiting for $host:$port" >&2
  return 1
}

start_services() {
  echo "Starting user-service and store-service (host JVM)..."

  # Start user-service pointing at localhost:5433
  COMMON_DB_URL=jdbc:postgresql://localhost:5433/user_db \
  COMMON_DB_USER=user \
  COMMON_DB_PASS=password \
  SPRING_PROFILES_ACTIVE=docker \
  nohup java -jar user-service/target/user-service-0.1.0-SNAPSHOT.jar > user-service.log 2>&1 &
  echo $! > user-service.pid
  echo "user-service pid: $(cat user-service.pid)"

  # Start store-service pointing at localhost:5434
  COMMON_DB_URL=jdbc:postgresql://localhost:5434/store_db \
  COMMON_DB_USER=store \
  COMMON_DB_PASS=password \
  SPRING_PROFILES_ACTIVE=docker \
  nohup java -jar store-service/target/store-service-0.1.0-SNAPSHOT.jar > store-service.log 2>&1 &
  echo $! > store-service.pid
  echo "store-service pid: $(cat store-service.pid)"
}

stop_services() {
  echo "Stopping services..."
  [ -f user-service.pid ] && kill "$(cat user-service.pid)" || true
  [ -f store-service.pid ] && kill "$(cat store-service.pid)" || true
  rm -f user-service.pid store-service.pid || true
}

status() {
  echo "Postgres containers:"
  docker ps --filter name=ds-postgres-user --format "table {{.Names}}	{{.Status}}	{{.Ports}}"
  docker ps --filter name=ds-postgres-store --format "table {{.Names}}	{{.Status}}	{{.Ports}}"
  echo; echo "Service PIDs:"; [ -f user-service.pid ] && echo "user: $(cat user-service.pid)" || echo "user: not running"; [ -f store-service.pid ] && echo "store: $(cat store-service.pid)" || echo "store: not running"
  echo; echo "Health endpoints:"; curl -sS http://localhost:8081/actuator/health || true; echo; curl -sS http://localhost:8082/actuator/health || true; echo
}

case ${1:-} in
  start)
    start_postgres
    # Wait for DBs
    wait_for_port localhost 5433
    wait_for_port localhost 5434
    start_services
    echo "Started services. Use '$0 status' to check health and logs."
    ;;
  stop)
    stop_services
    ;;
  status)
    status
    ;;
  *)
    echo "Usage: $0 {start|stop|status}"
    exit 2
    ;;
esac
