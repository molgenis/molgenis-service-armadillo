#!/usr/bin/env bash
#
# Start the Flower SuperLink (TLS) and serverapp superexec, then stream
# both of their logs in the foreground.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

docker info >/dev/null 2>&1 || fail "Docker is not running."
[ -f "$CERTS_DIR/server.crt" ] || fail "TLS certs not found in $CERTS_DIR. Run generate_flower_credentials (via test-flower-containers.sh)."

log "Starting superlink..."
docker run -d --rm \
  -p 9091:9091 \
  -p 9092:9092 \
  -p 9093:9093 \
  -v "$CERTS_DIR:/app/certs:ro" \
  --name "$SUPERLINK" \
  "$SUPERLINK_IMAGE" \
  --ssl-ca-certfile /app/certs/ca.crt \
  --ssl-certfile /app/certs/server.crt \
  --ssl-keyfile /app/certs/server.key \
  --enable-supernode-auth \
  --isolation process

sleep 2

log "Starting serverapp superexec..."
# The ServerAppIo API (9091) uses the SuperLink's --appio-ssl-* certs, which
# we do not set, so it is plaintext; connect insecurely.
docker run -d --rm \
  --name "$SERVERAPP" \
  "$SUPEREXEC_IMAGE" \
  --insecure \
  --plugin-type serverapp \
  --appio-api-address host.docker.internal:9091 \
  --allow-runtime-dependency-installation

wait_for_container_running "$SERVERAPP"

log "Streaming logs (Ctrl+C to stop tailing — containers keep running)."
docker logs -f "$SUPERLINK"  2>&1 | sed "s/^/[$SUPERLINK] /"  &
docker logs -f "$SERVERAPP"  2>&1 | sed "s/^/[$SERVERAPP] /"  &
wait
