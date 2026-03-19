#!/usr/bin/env bash
#
# Tails logs from all flower test containers in a single stream.
# Similar to `docker compose logs -f`.
#
# Usage:
#   ./scripts/flower-test/logs.sh          # tail all containers
#   ./scripts/flower-test/logs.sh 50       # show last 50 lines per container
#
set -euo pipefail

TAIL="${1:-20}"

CONTAINERS=(
  flower-test-superlink
  flower-supernode-1
  flower-supernode-2
  flower-clientapp-1
  flower-clientapp-2
  flower-test-serverapp
)

for c in "${CONTAINERS[@]}"; do
  if docker inspect "$c" >/dev/null 2>&1; then
    docker logs --tail "$TAIL" -f "$c" 2>&1 | sed "s/^/[$c] /" &
  fi
done

wait
