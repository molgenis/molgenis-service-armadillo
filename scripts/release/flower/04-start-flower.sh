#!/usr/bin/env bash
#
# Step 3: Start Flower infrastructure — superlink, supernodes, clientapps, serverapp.
#
# Creates container configs on both Armadillo instances and starts them via the API.
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

# --- Preflight ---------------------------------------------------------------

log "Checking prerequisites..."
docker info >/dev/null 2>&1 || fail "Docker is not running."
curl -sf "$ARMADILLO_1_URL/actuator/health" >/dev/null 2>&1 || fail "Armadillo 1 not reachable on port $ARMADILLO_1_PORT"
curl -sf "$ARMADILLO_2_URL/actuator/health" >/dev/null 2>&1 || fail "Armadillo 2 not reachable on port $ARMADILLO_2_PORT"

# --- Start superlink ---------------------------------------------------------

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

# --- Create container configs on Armadillo 1 ---------------------------------

log "Creating container configs on Armadillo 1..."

put_container $ARMADILLO_1_PORT "$(cat <<'EOF'
{
  "type": "flower-supernode",
  "name": "flower-supernode-1",
  "image": "flwr/supernode:1.23.0",
  "dockerArgs": [
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=0 num-partitions=2 node-name='node1'",
    "--clientappio-api-address", "0.0.0.0:9094",
    "--isolation", "process"
  ]
}
EOF
)"

put_container $ARMADILLO_1_PORT "$(cat <<'EOF'
{
  "type": "flower-superexec",
  "name": "flower-clientapp-1",
  "image": "timmyjc/superexec-data-test:0.0.1",
  "dockerArgs": [
    "--insecure",
    "--plugin-type", "clientapp",
    "--appio-api-address", "flower-supernode-1:9094"
  ]
}
EOF
)"

# --- Create container configs on Armadillo 2 ---------------------------------

log "Creating container configs on Armadillo 2..."

put_container $ARMADILLO_2_PORT "$(cat <<'EOF'
{
  "type": "flower-supernode",
  "name": "flower-supernode-2",
  "image": "flwr/supernode:1.23.0",
  "dockerArgs": [
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=1 num-partitions=2 node-name='node2'",
    "--clientappio-api-address", "0.0.0.0:9095",
    "--isolation", "process"
  ]
}
EOF
)"

put_container $ARMADILLO_2_PORT "$(cat <<'EOF'
{
  "type": "flower-superexec",
  "name": "flower-clientapp-2",
  "image": "timmyjc/superexec-data-test:0.0.1",
  "dockerArgs": [
    "--insecure",
    "--plugin-type", "clientapp",
    "--appio-api-address", "flower-supernode-2:9095"
  ]
}
EOF
)"

# --- Start containers --------------------------------------------------------

start_container $ARMADILLO_1_PORT "$SUPERNODE_1"
wait_for_container_running "$SUPERNODE_1"

start_container $ARMADILLO_2_PORT "$SUPERNODE_2"
wait_for_container_running "$SUPERNODE_2"

# Give supernodes time to register with superlink
sleep 3

start_container $ARMADILLO_1_PORT "$CLIENTAPP_1"
wait_for_container_running "$CLIENTAPP_1"

start_container $ARMADILLO_2_PORT "$CLIENTAPP_2"
wait_for_container_running "$CLIENTAPP_2"

log "All flower containers started."

# --- Start serverapp superexec -----------------------------------------------

log "Starting serverapp superexec..."
docker run -d --rm \
  --name "$SERVERAPP" \
  "$SUPEREXEC_IMAGE" \
  --insecure \
  --plugin-type serverapp \
  --appio-api-address host.docker.internal:9091

wait_for_container_running "$SERVERAPP"

log ""
log "Flower infrastructure is running."
log "Container states:"
docker ps --filter "name=flower-" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}"
