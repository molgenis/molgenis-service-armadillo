#!/usr/bin/env bash
#
# Start the Flower SuperLink and serverapp superexec, then stream both
# of their logs in the foreground.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

docker info >/dev/null 2>&1 || fail "Docker is not running."

log "Starting superlink..."
docker run -d --rm \
  -p 9091:9091 \
  -p 9092:9092 \
  -p 9093:9093 \
  --name "$SUPERLINK" \
  "$SUPERLINK_IMAGE" \
  --insecure \
  --isolation process

sleep 2

log "Starting serverapp superexec..."
docker run -d --rm \
  --name "$SERVERAPP" \
  "$SUPEREXEC_IMAGE" \
  --insecure \
  --plugin-type serverapp \
  --appio-api-address host.docker.internal:9091

wait_for_container_running "$SERVERAPP"

log "Streaming logs (Ctrl+C to stop tailing — containers keep running)."
docker logs -f "$SUPERLINK"  2>&1 | sed "s/^/[$SUPERLINK] /"  &
docker logs -f "$SERVERAPP"  2>&1 | sed "s/^/[$SERVERAPP] /"  &
wait