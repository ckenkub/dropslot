#!/usr/bin/env bash
set -euo pipefail
# Build backend modules with Jib into the local Docker daemon for local Jib validation.
# Usage:
#   ./scripts/jib-local-test.sh all                -> builds discovery, api-gateway, user-service, store-service
#   ./scripts/jib-local-test.sh user-service       -> builds only user-service (and its module deps)
#   ./scripts/jib-local-test.sh user-service store-service -> builds the listed modules
# Optional: set TAG=custom-tag to override image tags (default: local-jib-<timestamp>)

REPO_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$REPO_ROOT/backend"

DEFAULT_TAG="local-jib-$(date +%s)"
TAG=${TAG:-$DEFAULT_TAG}

ALL_MODULES=(discovery-service api-gateway user-service store-service)

if [ "$#" -eq 0 ]; then
  echo "No modules specified, building all..."
  MODULES=("${ALL_MODULES[@]}")
else
  if [ "$1" = "all" ]; then
    MODULES=("${ALL_MODULES[@]}")
  else
    MODULES=("$@")
  fi
fi

echo "Jib local test: modules=${MODULES[*]} tag=${TAG}"

for mod in "${MODULES[@]}"; do
  echo
  echo "===== Building $mod with Jib to Docker (tag: $TAG) ====="
  # Use -pl to build selected module; -am to also build module dependencies
  # Use jib:dockerBuild to write the image to the local Docker daemon (no push)
  mvn -B -DskipTests -pl "$mod" -am jib:dockerBuild -Djib.to.image=docker://dropslot/$mod:$TAG
  echo "Built dropslot/$mod:$TAG -> available in local Docker daemon"
done

echo
echo "All requested modules built. Use 'docker images | grep dropslot' to list images."
