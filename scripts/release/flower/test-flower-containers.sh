#!/usr/bin/env bash
#
# Integration test for Flower container management via Armadillo.
#
# Starts a superlink, two Armadillo instances (simulating two data nodes),
# then uses the Armadillo API to create and start flower-supernode and
# flower-superexec (clientapp) containers on each. Runs three scenarios:
#
#   Scenario A — Baseline: standard superlink + supernodes, unsigned flwr run
#   Scenario B — Signed FAB: standard superlink + verified supernodes, signed FAB
#   Scenario C — Unsigned FAB rejected: verified supernodes reject unsigned FAB
#
# Prerequisites:
#   - Docker running
#   - Armadillo bootJar built: ./gradlew bootJar
#   - Java 17+
#   - flwr CLI installed (pip install flwr)
#   - molgenis-flwr-armadillo installed (pip install -e ../molgenis-flwr-armadillo)
#   - Superexec image pushed to Docker Hub (see build-push-superexec.sh)
#
# Usage:
#   ./scripts/release/flower/test-flower-containers.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
FLWR_ARMADILLO_DIR="${FLWR_ARMADILLO_DIR:-$(cd "$SCRIPT_DIR/../../../../molgenis-flwr-armadillo" && pwd)}"

# --- Configuration -----------------------------------------------------------

ARMADILLO_JAR="$PROJECT_ROOT/build/libs/molgenis-armadillo-5.13.0-SNAPSHOT.jar"
SUPERLINK_IMAGE="flwr/superlink:1.23.0"
VERIFIED_SUPERNODE_IMAGE="molgenis/verified-supernode:test"
FLWR_APP_DIR="${FLWR_APP_DIR:-$SCRIPT_DIR/quickstart-pytorch}"

ADMIN_USER="admin"
ADMIN_PASS="admin"

ARMADILLO_1_PORT=8080
ARMADILLO_2_PORT=8081
ARMADILLO_1_DATA="$SCRIPT_DIR/data1"
ARMADILLO_2_DATA="$SCRIPT_DIR/data2"

FLOWER_NETWORK="flower-network"

# Container names
SUPERNODE_1="flower-supernode-1"
SUPERNODE_2="flower-supernode-2"
CLIENTAPP_1="flower-clientapp-1"
CLIENTAPP_2="flower-clientapp-2"
SUPERLINK="flower-test-superlink"
SERVERAPP="flower-test-serverapp"
SUPEREXEC_IMAGE="timmyjc/superexec-test:0.0.1"

# PIDs for cleanup
ARMADILLO_1_PID=""
ARMADILLO_2_PID=""

# Signing test artifacts
SIGNING_DIR="/tmp/flower-signing-test"

# --- Helpers -----------------------------------------------------------------

log()  { echo ">>> $*"; }
fail() { echo "FAIL: $*" >&2; exit 1; }

cleanup() {
  log "Cleaning up..."

  # Stop Armadillo instances
  for pid in $ARMADILLO_1_PID $ARMADILLO_2_PID; do
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
    fi
  done

  # Stop Docker containers
  for c in $SERVERAPP $CLIENTAPP_1 $CLIENTAPP_2 $SUPERNODE_1 $SUPERNODE_2 $SUPERLINK; do
    docker rm -f "$c" 2>/dev/null || true
  done

  # Remove network
  docker network rm "$FLOWER_NETWORK" 2>/dev/null || true

  # Remove temp data dirs and signing artifacts
  rm -rf "$ARMADILLO_1_DATA" "$ARMADILLO_2_DATA" "$SIGNING_DIR"

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

stop_and_remove_container() {
  local port=$1
  local name=$2
  log "Stopping container '$name'..."
  curl -s -o /dev/null -u "$ADMIN_USER:$ADMIN_PASS" \
    -X POST "http://localhost:$port/containers/$name/stop" 2>/dev/null || true
  docker rm -f "$name" 2>/dev/null || true
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

wait_for_training() {
  local container=$1
  local max_wait=${2:-300}
  local wait_elapsed=0
  log "Waiting for training to complete (checking $container logs)..."
  while true; do
    if docker logs "$container" 2>&1 | grep -q "Saving final model to disk"; then
      log "Training completed successfully."
      return 0
    fi
    wait_elapsed=$((wait_elapsed + 5))
    if [ $wait_elapsed -ge $max_wait ]; then
      echo "--- $container logs ---"
      docker logs "$container" 2>&1 | tail -40
      return 1
    fi
    sleep 5
  done
}

show_failure_logs() {
  echo "--- supernode-1 logs ---"
  docker logs "$SUPERNODE_1" 2>&1 | tail -30
  echo "--- supernode-2 logs ---"
  docker logs "$SUPERNODE_2" 2>&1 | tail -30
  echo "--- clientapp-1 logs ---"
  docker logs "$CLIENTAPP_1" 2>&1 | tail -30
  echo "--- clientapp-2 logs ---"
  docker logs "$CLIENTAPP_2" 2>&1 | tail -30
  echo "--- serverapp logs ---"
  docker logs "$SERVERAPP" 2>&1 | tail -30
}

# Tear down flower containers (superlink, supernodes, clientapps, serverapp)
# but keep Armadillo instances running.
teardown_flower() {
  log "Tearing down flower containers..."
  for c in $SERVERAPP $CLIENTAPP_1 $CLIENTAPP_2 $SUPERNODE_1 $SUPERNODE_2 $SUPERLINK; do
    docker rm -f "$c" 2>/dev/null || true
  done
  sleep 2
}

# Start standard (unverified) supernodes + clientapps via Armadillo API
start_standard_supernodes() {
  log "Creating standard supernode configs..."

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
  "image": "timmyjc/superexec-test:0.0.1",
  "dockerArgs": [
    "--insecure",
    "--plugin-type", "clientapp",
    "--appio-api-address", "flower-supernode-1:9094"
  ]
}
EOF
)"

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
  "image": "timmyjc/superexec-test:0.0.1",
  "dockerArgs": [
    "--insecure",
    "--plugin-type", "clientapp",
    "--appio-api-address", "flower-supernode-2:9095"
  ]
}
EOF
)"

  start_container $ARMADILLO_1_PORT "$SUPERNODE_1"
  wait_for_container_running "$SUPERNODE_1"
  start_container $ARMADILLO_2_PORT "$SUPERNODE_2"
  wait_for_container_running "$SUPERNODE_2"

  sleep 3

  start_container $ARMADILLO_1_PORT "$CLIENTAPP_1"
  wait_for_container_running "$CLIENTAPP_1"
  start_container $ARMADILLO_2_PORT "$CLIENTAPP_2"
  wait_for_container_running "$CLIENTAPP_2"

  log "Standard supernodes started."
}

# Start verified supernodes (with trusted-entities.yaml) + clientapps
start_verified_supernodes() {
  local trusted_entities_path=$1
  log "Creating verified supernode configs..."

  put_container $ARMADILLO_1_PORT "$(cat <<EOF
{
  "type": "flower-supernode",
  "name": "flower-supernode-1",
  "image": "$VERIFIED_SUPERNODE_IMAGE",
  "dockerArgs": [
    "--trusted-entities", "/app/trusted-entities.yaml",
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=0 num-partitions=2 node-name='node1'",
    "--clientappio-api-address", "0.0.0.0:9094",
    "--isolation", "process"
  ],
  "volumes": {
    "$trusted_entities_path": "/app/trusted-entities.yaml"
  }
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

  put_container $ARMADILLO_2_PORT "$(cat <<EOF
{
  "type": "flower-supernode",
  "name": "flower-supernode-2",
  "image": "$VERIFIED_SUPERNODE_IMAGE",
  "dockerArgs": [
    "--trusted-entities", "/app/trusted-entities.yaml",
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=1 num-partitions=2 node-name='node2'",
    "--clientappio-api-address", "0.0.0.0:9095",
    "--isolation", "process"
  ],
  "volumes": {
    "$trusted_entities_path": "/app/trusted-entities.yaml"
  }
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

  start_container $ARMADILLO_1_PORT "$SUPERNODE_1"
  wait_for_container_running "$SUPERNODE_1"
  start_container $ARMADILLO_2_PORT "$SUPERNODE_2"
  wait_for_container_running "$SUPERNODE_2"

  sleep 3

  start_container $ARMADILLO_1_PORT "$CLIENTAPP_1"
  wait_for_container_running "$CLIENTAPP_1"
  start_container $ARMADILLO_2_PORT "$CLIENTAPP_2"
  wait_for_container_running "$CLIENTAPP_2"

  log "Verified supernodes started."
}

start_superlink() {
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
}

start_serverapp() {
  log "Starting serverapp superexec..."
  docker run -d --rm \
    --name "$SERVERAPP" \
    "$SUPEREXEC_IMAGE" \
    --insecure \
    --plugin-type serverapp \
    --appio-api-address host.docker.internal:9091
  wait_for_container_running "$SERVERAPP"
}

# --- Preflight checks --------------------------------------------------------

log "Checking prerequisites..."

[ -f "$ARMADILLO_JAR" ] || fail "Armadillo JAR not found at $ARMADILLO_JAR. Run: ./gradlew bootJar"

docker info >/dev/null 2>&1 || fail "Docker is not running."

command -v flwr >/dev/null 2>&1 || fail "flwr CLI not found. Install with: pip install flwr"

command -v molgenis-flwr-keygen >/dev/null 2>&1 || fail "molgenis-flwr-keygen not found. Install with: pip install -e molgenis-flwr-armadillo"

[ -d "$FLWR_APP_DIR" ] || fail "Flower app directory not found at $FLWR_APP_DIR"

[ -d "$FLWR_ARMADILLO_DIR" ] || fail "molgenis-flwr-armadillo not found at $FLWR_ARMADILLO_DIR"

# --- Signing setup (shared across scenarios B and C) -------------------------

log "Setting up signing artifacts..."
mkdir -p "$SIGNING_DIR"

# Generate keypair
molgenis-flwr-keygen --name "$SIGNING_DIR/test-steward"
log "Keypair generated."

# Build trusted-entities.yaml from the public key
python3 -c "
from molgenis_flwr_armadillo.signing import derive_key_id
from cryptography.hazmat.primitives.serialization import load_pem_public_key
import yaml

pub = load_pem_public_key(open('$SIGNING_DIR/test-steward.pub', 'rb').read())
key_id = derive_key_id(pub)
pub_pem = open('$SIGNING_DIR/test-steward.pub').read()
yaml.dump({key_id: pub_pem}, open('$SIGNING_DIR/trusted-entities.yaml', 'w'))
print(f'trusted-entities.yaml created with key_id={key_id}')
"

# Sign the FAB
molgenis-flwr-sign --app-dir "$FLWR_APP_DIR" \
  --private-key "$SIGNING_DIR/test-steward.key" \
  --output "$SIGNING_DIR/study-a.sfab"
log "Signed FAB created at $SIGNING_DIR/study-a.sfab"

# Build verified-supernode Docker image
log "Building verified-supernode Docker image..."
docker build \
  -f "$FLWR_ARMADILLO_DIR/docker/verified-supernode.Dockerfile" \
  -t "$VERIFIED_SUPERNODE_IMAGE" \
  "$FLWR_ARMADILLO_DIR"

# --- Start Armadillo instances (shared across all scenarios) -----------------

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

wait_for_armadillo $ARMADILLO_1_PORT
wait_for_armadillo $ARMADILLO_2_PORT

# =============================================================================
# Scenario A — Baseline: standard supernodes, unsigned flwr run
# =============================================================================

log ""
log "========================================="
log "  SCENARIO A: Baseline (unsigned FAB)"
log "========================================="
log ""

start_superlink
start_standard_supernodes
start_serverapp

log "Running FL job via 'flwr run --stream'..."
(cd "$FLWR_APP_DIR" && flwr run . local-deployment --stream 2>&1 | tee "$SCRIPT_DIR/flwr-run-a.log")
FLWR_EXIT=${PIPESTATUS[0]}

if [ "$FLWR_EXIT" -ne 0 ]; then
  log "flwr run exited with code $FLWR_EXIT"
  show_failure_logs
  fail "Scenario A: FL job failed."
fi

if ! wait_for_training "$SERVERAPP"; then
  fail "Scenario A: Training did not complete."
fi

log "Scenario A PASSED: baseline training completed."

teardown_flower

# =============================================================================
# Scenario B — Signed FAB accepted by verified supernodes
# =============================================================================

log ""
log "========================================="
log "  SCENARIO B: Signed FAB (accepted)"
log "========================================="
log ""

start_superlink
start_verified_supernodes "$SIGNING_DIR/trusted-entities.yaml"
start_serverapp

log "Submitting signed FAB via molgenis-flwr-run..."
molgenis-flwr-run \
  --signed-fab "$SIGNING_DIR/study-a.sfab" \
  --federation-address 127.0.0.1:9093 \
  2>&1 | tee "$SCRIPT_DIR/flwr-run-b.log"

if ! wait_for_training "$SERVERAPP"; then
  show_failure_logs
  fail "Scenario B: Training did not complete with signed FAB."
fi

log "Scenario B PASSED: signed FAB accepted, training completed."

teardown_flower

# =============================================================================
# Scenario C — Unsigned FAB rejected by verified supernodes
# =============================================================================

log ""
log "========================================="
log "  SCENARIO C: Unsigned FAB (rejected)"
log "========================================="
log ""

start_superlink
start_verified_supernodes "$SIGNING_DIR/trusted-entities.yaml"
start_serverapp

log "Running unsigned FL job via 'flwr run --stream' (should be rejected)..."
(cd "$FLWR_APP_DIR" && flwr run . local-deployment --stream 2>&1 | tee "$SCRIPT_DIR/flwr-run-c.log") || true

# Wait briefly for the supernodes to attempt to pull and verify the FAB
sleep 15

# Check supernode logs for rejection
REJECTED=false
for sn in "$SUPERNODE_1" "$SUPERNODE_2"; do
  if docker logs "$sn" 2>&1 | grep -q "REJECTED FAB\|signature verification failed"; then
    log "Verified: $sn rejected unsigned FAB"
    REJECTED=true
  fi
done

if [ "$REJECTED" = "false" ]; then
  echo "--- supernode-1 logs ---"
  docker logs "$SUPERNODE_1" 2>&1 | tail -30
  echo "--- supernode-2 logs ---"
  docker logs "$SUPERNODE_2" 2>&1 | tail -30
  fail "Scenario C: Expected supernodes to reject unsigned FAB, but no rejection found."
fi

# Verify training did NOT complete
if docker logs "$SERVERAPP" 2>&1 | grep -q "Saving final model to disk"; then
  fail "Scenario C: Training should NOT have completed with unsigned FAB."
fi

log "Scenario C PASSED: unsigned FAB rejected by verified supernodes."

teardown_flower

# =============================================================================
# Final summary
# =============================================================================

log ""
log "========================================="
log "  ALL SCENARIOS PASSED"
log "========================================="
log ""
log "  A: Baseline (standard supernodes, unsigned FAB) — OK"
log "  B: Signed FAB accepted by verified supernodes   — OK"
log "  C: Unsigned FAB rejected by verified supernodes  — OK"
log ""
