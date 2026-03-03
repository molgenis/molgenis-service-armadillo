#!/usr/bin/env bash
#
# Integration test for Flower container management via Armadillo.
#
# Starts a superlink, three Armadillo instances (simulating three data nodes),
# then uses the Armadillo API to create and start flower-supernode and
# flower-superexec (clientapp) containers on each. Finally runs the serverapp
# to trigger a federated learning job and checks for success.
#
# Nodes 1+2 run app-id=study-a (process mode with separate clientapp).
# Node 3 runs app-id=study-b (subprocess mode with app baked into supernode).
# The discovery protocol verifies that only nodes 1+2 participate in training.
#
# Prerequisites:
#   - Docker running
#   - Armadillo bootJar built: ./gradlew bootJar
#   - Java 17+
#   - flwr CLI installed (pip install flwr)
#   - Superexec image pushed to Docker Hub (see build-push-superexec.sh)
#
# Usage:
#   ./scripts/flower-test/test-flower-containers.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# --- Configuration -----------------------------------------------------------

ARMADILLO_JAR="$PROJECT_ROOT/build/libs/molgenis-armadillo-5.13.0-SNAPSHOT.jar"
SUPERLINK_IMAGE="flwr/superlink:1.23.0"
FLWR_APP_DIR="${FLWR_APP_DIR:-$SCRIPT_DIR/quickstart-pytorch}"

ADMIN_USER="admin"
ADMIN_PASS="admin"

ARMADILLO_1_PORT=8080
ARMADILLO_2_PORT=8081
ARMADILLO_3_PORT=8082
ARMADILLO_1_DATA="$SCRIPT_DIR/data1"
ARMADILLO_2_DATA="$SCRIPT_DIR/data2"
ARMADILLO_3_DATA="$SCRIPT_DIR/data3"

FLOWER_NETWORK="flower-network"

# Container names (must be unique across all Armadillo instances)
SUPERNODE_1="flower-supernode-1"
SUPERNODE_2="flower-supernode-2"
SUPERNODE_3="flower-supernode-3"
CLIENTAPP_1="flower-clientapp-1"
CLIENTAPP_2="flower-clientapp-2"
SUPERLINK="flower-test-superlink"
SERVERAPP="flower-test-serverapp"
SUPEREXEC_IMAGE="timmyjc/superexec-test:0.0.1"
SUPERNODE_APP_IMAGE="timmyjc/supernode-app-test:0.0.1"

# PIDs for cleanup
ARMADILLO_1_PID=""
ARMADILLO_2_PID=""
ARMADILLO_3_PID=""

# --- Helpers -----------------------------------------------------------------

log()  { echo ">>> $*"; }
fail() { echo "FAIL: $*" >&2; exit 1; }

cleanup() {
  log "Cleaning up..."

  # Stop Armadillo instances
  for pid in $ARMADILLO_1_PID $ARMADILLO_2_PID $ARMADILLO_3_PID; do
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
    fi
  done

  # Stop Docker containers
  for c in $SERVERAPP $CLIENTAPP_1 $CLIENTAPP_2 $SUPERNODE_1 $SUPERNODE_2 $SUPERNODE_3 $SUPERLINK; do
    docker rm -f "$c" 2>/dev/null || true
  done

  # Remove network
  docker network rm "$FLOWER_NETWORK" 2>/dev/null || true

  # Remove temp data dirs
  rm -rf "$ARMADILLO_1_DATA" "$ARMADILLO_2_DATA" "$ARMADILLO_3_DATA"

  log "Cleanup done."
}

trap cleanup EXIT

wait_for_armadillo() {
  local port=$1
  local max_wait=30
  local i=0
  while ! curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; do
    i=$((i + 1))
    if [ $i -ge $max_wait ]; then
      fail "Armadillo on port $port did not start within ${max_wait}s"
    fi
    sleep 1
  done
  log "Armadillo on port $port is ready."
}

put_container() {
  local port=$1
  local json=$2
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -u "$ADMIN_USER:$ADMIN_PASS" \
    -X PUT \
    -H "Content-Type: application/json" \
    -d "$json" \
    "http://localhost:$port/containers")
  if [ "$http_code" != "204" ] && [ "$http_code" != "200" ]; then
    fail "PUT /containers on port $port returned HTTP $http_code"
  fi
}

start_container() {
  local port=$1
  local name=$2
  log "Starting container '$name' via Armadillo on port $port..."
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -u "$ADMIN_USER:$ADMIN_PASS" \
    -X POST \
    "http://localhost:$port/containers/$name/start")
  if [ "$http_code" != "204" ] && [ "$http_code" != "200" ]; then
    fail "POST /containers/$name/start on port $port returned HTTP $http_code"
  fi
}

wait_for_container_running() {
  local name=$1
  local max_wait=30
  local i=0
  while true; do
    local state
    state=$(docker inspect -f '{{.State.Status}}' "$name" 2>/dev/null || echo "missing")
    if [ "$state" = "running" ]; then
      log "Container '$name' is running."
      return 0
    fi
    i=$((i + 1))
    if [ $i -ge $max_wait ]; then
      fail "Container '$name' not running after ${max_wait}s (state: $state)"
    fi
    sleep 1
  done
}

# --- Preflight checks --------------------------------------------------------

log "Checking prerequisites..."

[ -f "$ARMADILLO_JAR" ] || fail "Armadillo JAR not found at $ARMADILLO_JAR. Run: ./gradlew bootJar"

docker info >/dev/null 2>&1 || fail "Docker is not running."

command -v flwr >/dev/null 2>&1 || fail "flwr CLI not found. Install with: pip install flwr"

[ -d "$FLWR_APP_DIR" ] || fail "Flower app directory not found at $FLWR_APP_DIR"

# --- Step 1: Start superlink -------------------------------------------------

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

# --- Step 2: Start Armadillo instances ----------------------------------------

log "Starting Armadillo instance 1 (port $ARMADILLO_1_PORT)..."
mkdir -p "$ARMADILLO_1_DATA"
java -jar "$ARMADILLO_JAR" \
  --server.port=$ARMADILLO_1_PORT \
  --storage.root-dir="$ARMADILLO_1_DATA" \
  --spring.security.user.name=$ADMIN_USER \
  --spring.security.user.password=$ADMIN_PASS \
  --armadillo.docker-management-enabled=true \
  > "$SCRIPT_DIR/armadillo1.log" 2>&1 &
ARMADILLO_1_PID=$!

log "Starting Armadillo instance 2 (port $ARMADILLO_2_PORT)..."
mkdir -p "$ARMADILLO_2_DATA"
java -jar "$ARMADILLO_JAR" \
  --server.port=$ARMADILLO_2_PORT \
  --storage.root-dir="$ARMADILLO_2_DATA" \
  --spring.security.user.name=$ADMIN_USER \
  --spring.security.user.password=$ADMIN_PASS \
  --armadillo.docker-management-enabled=true \
  > "$SCRIPT_DIR/armadillo2.log" 2>&1 &
ARMADILLO_2_PID=$!

log "Starting Armadillo instance 3 (port $ARMADILLO_3_PORT)..."
mkdir -p "$ARMADILLO_3_DATA"
java -jar "$ARMADILLO_JAR" \
  --server.port=$ARMADILLO_3_PORT \
  --storage.root-dir="$ARMADILLO_3_DATA" \
  --spring.security.user.name=$ADMIN_USER \
  --spring.security.user.password=$ADMIN_PASS \
  --armadillo.docker-management-enabled=true \
  > "$SCRIPT_DIR/armadillo3.log" 2>&1 &
ARMADILLO_3_PID=$!

wait_for_armadillo $ARMADILLO_1_PORT
wait_for_armadillo $ARMADILLO_2_PORT
wait_for_armadillo $ARMADILLO_3_PORT

# --- Step 3: Create flower container configs via API --------------------------

log "Creating container configs on Armadillo 1..."

put_container $ARMADILLO_1_PORT "$(cat <<'EOF'
{
  "type": "flower-supernode",
  "name": "flower-supernode-1",
  "image": "flwr/supernode:1.23.0",
  "dockerArgs": [
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=0 num-partitions=3 node-name='node1' app-id='study-a'",
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
  "image": "timmyjc/superexec-test:0.0.1",
  "dockerArgs": [
    "--insecure",
    "--plugin-type", "clientapp",
    "--appio-api-address", "flower-supernode-1:9094"
  ]
}
EOF
)"

log "Creating container configs on Armadillo 2..."

put_container $ARMADILLO_2_PORT "$(cat <<'EOF'
{
  "type": "flower-supernode",
  "name": "flower-supernode-2",
  "image": "flwr/supernode:1.23.0",
  "dockerArgs": [
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=1 num-partitions=3 node-name='node2' app-id='study-a'",
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
  "image": "timmyjc/superexec-test:0.0.1",
  "dockerArgs": [
    "--insecure",
    "--plugin-type", "clientapp",
    "--appio-api-address", "flower-supernode-2:9095"
  ]
}
EOF
)"

log "Creating container configs on Armadillo 3..."

put_container $ARMADILLO_3_PORT "$(cat <<'EOF'
{
  "type": "flower-supernode",
  "name": "flower-supernode-3",
  "image": "timmyjc/supernode-app-test:0.0.1",
  "dockerArgs": [
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=2 num-partitions=3 node-name='node3' app-id='study-b'",
    "--isolation", "subprocess",
    "--app-dir", "/app"
  ]
}
EOF
)"

# --- Step 4: Start containers via API -----------------------------------------

start_container $ARMADILLO_1_PORT "$SUPERNODE_1"
wait_for_container_running "$SUPERNODE_1"

start_container $ARMADILLO_2_PORT "$SUPERNODE_2"
wait_for_container_running "$SUPERNODE_2"

start_container $ARMADILLO_3_PORT "$SUPERNODE_3"
wait_for_container_running "$SUPERNODE_3"

# Give supernodes a moment to register with superlink
sleep 3

start_container $ARMADILLO_1_PORT "$CLIENTAPP_1"
wait_for_container_running "$CLIENTAPP_1"

start_container $ARMADILLO_2_PORT "$CLIENTAPP_2"
wait_for_container_running "$CLIENTAPP_2"

log "All flower containers started."

# --- Step 5: Start serverapp superexec ----------------------------------------

log "Starting serverapp superexec (connects to superlink)..."
docker run -d --rm \
  --name "$SERVERAPP" \
  "$SUPEREXEC_IMAGE" \
  --insecure \
  --plugin-type serverapp \
  --appio-api-address host.docker.internal:9091

wait_for_container_running "$SERVERAPP"

# --- Step 6: Run the FL job via flwr CLI -------------------------------------

log "Running FL job via 'flwr run --stream' (local-deployment federation)..."
log "App directory: $FLWR_APP_DIR"

(cd "$FLWR_APP_DIR" && flwr run . local-deployment --stream --run-config 'app-id="study-a"' 2>&1 | tee "$SCRIPT_DIR/flwr-run.log")
FLWR_EXIT=${PIPESTATUS[0]}

if [ "$FLWR_EXIT" -ne 0 ]; then
  log "flwr run exited with code $FLWR_EXIT"
  echo "--- supernode-1 logs ---"
  docker logs "$SUPERNODE_1" 2>&1 | tail -30
  echo "--- supernode-2 logs ---"
  docker logs "$SUPERNODE_2" 2>&1 | tail -30
  echo "--- supernode-3 logs ---"
  docker logs "$SUPERNODE_3" 2>&1 | tail -30
  echo "--- clientapp-1 logs ---"
  docker logs "$CLIENTAPP_1" 2>&1 | tail -30
  echo "--- clientapp-2 logs ---"
  docker logs "$CLIENTAPP_2" 2>&1 | tail -30
  echo "--- serverapp logs ---"
  docker logs "$SERVERAPP" 2>&1 | tail -30
  fail "FL job failed. See logs above."
fi

log "flwr run completed."

# --- Step 7: Wait for training to finish -------------------------------------

log "Waiting for training to complete (checking serverapp logs)..."
MAX_TRAIN_WAIT=300
TRAIN_WAIT=0
while true; do
  if docker logs "$SERVERAPP" 2>&1 | grep -q "Saving final model to disk"; then
    log "Training completed successfully — all rounds finished."
    break
  fi
  TRAIN_WAIT=$((TRAIN_WAIT + 5))
  if [ $TRAIN_WAIT -ge $MAX_TRAIN_WAIT ]; then
    echo "--- serverapp logs ---"
    docker logs "$SERVERAPP" 2>&1 | tail -40
    fail "Training did not complete within ${MAX_TRAIN_WAIT}s"
  fi
  sleep 5
done

# --- Step 8: Verify ----------------------------------------------------------

# Verify discovery filtered to 2 nodes
if docker logs "$SERVERAPP" 2>&1 | grep -q "Discovered 2 nodes"; then
  log "Discovery filtering verified: only 2 nodes matched app-id=study-a"
else
  echo "--- serverapp logs ---"
  docker logs "$SERVERAPP" 2>&1 | tail -40
  fail "Expected 'Discovered 2 nodes' in serverapp logs"
fi

log "Checking container states..."
for c in $SUPERNODE_1 $SUPERNODE_2 $SUPERNODE_3 $CLIENTAPP_1 $CLIENTAPP_2; do
  state=$(docker inspect -f '{{.State.Status}}' "$c" 2>/dev/null || echo "missing")
  if [ "$state" != "running" ] && [ "$state" != "exited" ]; then
    echo "--- $c logs ---"
    docker logs "$c" 2>&1 | tail -20
    fail "Container '$c' is in unexpected state: $state"
  fi
  log "Container '$c': $state"
done

log ""
log "========================================="
log "  Flower integration test PASSED"
log "========================================="
log ""
log "Container states:"
docker ps --filter "name=flower-" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}"
