#!/usr/bin/env bash
#
# Integration test for Flower data loading via Armadillo push-data endpoint.
#
# This test:
#   1. Starts two Armadillo instances with OIDC auth enabled
#   2. Authenticates via device flow (opens browser)
#   3. Prepares CIFAR10 test data and uploads to Armadillo storage
#   4. Runs a FL job where ClientApps use load_data() to fetch data via
#      POST /flower/push-data (Armadillo reads from storage, Docker-copies
#      into container at /tmp/armadillo_data/)
#
# Prerequisites:
#   - Docker running
#   - Armadillo bootJar built: ./gradlew bootJar
#   - Java 17+
#   - flwr CLI installed (pip install flwr)
#   - molgenis-flwr-armadillo installed (pip install -e ../molgenis-flwr-armadillo)
#   - Superexec image pushed (see build-push-superexec.sh)
#   - Python with torch + torchvision for split_data.py
#
# Usage:
#   ./scripts/release/flower/test-flower-data.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# --- Configuration -----------------------------------------------------------

ARMADILLO_JAR="$PROJECT_ROOT/build/libs/molgenis-armadillo-5.13.0-SNAPSHOT.jar"
SUPERLINK_IMAGE="flwr/superlink:1.23.0"
FLWR_APP_DIR="${FLWR_APP_DIR:-$SCRIPT_DIR/quickstart-pytorch-data}"

ADMIN_USER="admin"
ADMIN_PASS="admin"

# OIDC settings (auth.molgenis.org)
OIDC_ISSUER_URI="https://auth.molgenis.org"
OIDC_CLIENT_ID="b396233b-cdb2-449e-ac5c-a0d28b38f791"
OIDC_CLIENT_SECRET="nRO_t1_cIpdzRzr-cWBeZg4ckBsMHmX2MlA9SaSg4P8"

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
SUPEREXEC_IMAGE="timmyjc/superexec-data-test:0.0.1"

PROJECT_NAME="test-flower"
TOKEN_FILE="/tmp/flwr_tokens.json"
NODES_CONFIG="$SCRIPT_DIR/flower-nodes.yaml"

# PIDs for cleanup
ARMADILLO_1_PID=""
ARMADILLO_2_PID=""

# --- Helpers -----------------------------------------------------------------

log()  { echo ">>> $*"; }
fail() { echo "FAIL: $*" >&2; exit 1; }

cleanup() {
  log "Cleaning up..."

  for pid in $ARMADILLO_1_PID $ARMADILLO_2_PID; do
    if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
      kill "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
    fi
  done

  for c in $SERVERAPP $CLIENTAPP_1 $CLIENTAPP_2 $SUPERNODE_1 $SUPERNODE_2 $SUPERLINK; do
    docker rm -f "$c" 2>/dev/null || true
  done

  docker network rm "$FLOWER_NETWORK" 2>/dev/null || true
  rm -rf "$ARMADILLO_1_DATA" "$ARMADILLO_2_DATA"
  rm -f "$SCRIPT_DIR/cifar10_train.pt" "$SCRIPT_DIR/cifar10_test.pt"
  rm -f "$NODES_CONFIG"

  log "Cleanup done."
}

trap cleanup EXIT

wait_for_armadillo() {
  local port=$1
  local max_wait=60
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

upload_to_storage() {
  local port=$1
  local project=$2
  local object_path=$3
  local file_path=$4
  log "Uploading $file_path to $project/$object_path on port $port..."
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -u "$ADMIN_USER:$ADMIN_PASS" \
    -X POST \
    -F "file=@$file_path" \
    "http://localhost:$port/storage/projects/$project/objects" \
    -F "object=$object_path")
  if [ "$http_code" != "204" ] && [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
    fail "Upload to storage on port $port returned HTTP $http_code"
  fi
  log "Upload complete."
}

create_project() {
  local port=$1
  local project=$2
  log "Creating project '$project' on port $port..."
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -u "$ADMIN_USER:$ADMIN_PASS" \
    -X PUT \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"$project\"}" \
    "http://localhost:$port/access/projects")
  if [ "$http_code" != "204" ] && [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
    fail "Create project on port $port returned HTTP $http_code"
  fi
}

# --- Preflight checks --------------------------------------------------------

log "Checking prerequisites..."

[ -f "$ARMADILLO_JAR" ] || fail "Armadillo JAR not found at $ARMADILLO_JAR. Run: ./gradlew bootJar"
docker info >/dev/null 2>&1 || fail "Docker is not running."
command -v flwr >/dev/null 2>&1 || fail "flwr CLI not found. Install with: pip install flwr"
command -v python3 >/dev/null 2>&1 || fail "python3 not found."
command -v molgenis-flwr-authenticate >/dev/null 2>&1 || fail "molgenis-flwr-authenticate not found. Install with: pip install -e ../molgenis-flwr-armadillo"
[ -d "$FLWR_APP_DIR" ] || fail "Flower app directory not found at $FLWR_APP_DIR"

# --- Step 1: Prepare test data -----------------------------------------------

log "Preparing CIFAR10 test data..."
(cd "$SCRIPT_DIR" && python3 "$FLWR_APP_DIR/pytorchexample/split_data.py")
[ -f "$SCRIPT_DIR/cifar10_train.pt" ] || fail "split_data.py did not create cifar10_train.pt"
[ -f "$SCRIPT_DIR/cifar10_test.pt" ] || fail "split_data.py did not create cifar10_test.pt"
log "Test data prepared."

# --- Step 2: Start superlink -------------------------------------------------

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

# --- Step 3: Start Armadillo instances with OIDC -----------------------------

log "Starting Armadillo instance 1 (port $ARMADILLO_1_PORT) with OIDC..."
mkdir -p "$ARMADILLO_1_DATA"
java -jar "$ARMADILLO_JAR" \
  --server.port=$ARMADILLO_1_PORT \
  --storage.root-dir="$ARMADILLO_1_DATA" \
  --spring.security.user.name=$ADMIN_USER \
  --spring.security.user.password=$ADMIN_PASS \
  --armadillo.docker-management-enabled=true \
  --spring.security.oauth2.client.provider.molgenis.issuer-uri="$OIDC_ISSUER_URI" \
  --spring.security.oauth2.client.registration.molgenis.client-id="$OIDC_CLIENT_ID" \
  --spring.security.oauth2.client.registration.molgenis.client-secret="$OIDC_CLIENT_SECRET" \
  --spring.security.oauth2.client.registration.molgenis.authorization-grant-type=authorization_code \
  --spring.security.oauth2.resourceserver.jwt.issuer-uri="$OIDC_ISSUER_URI" \
  --spring.security.oauth2.resourceserver.opaquetoken.client-id="$OIDC_CLIENT_ID" \
  > "$SCRIPT_DIR/armadillo1.log" 2>&1 &
ARMADILLO_1_PID=$!

log "Starting Armadillo instance 2 (port $ARMADILLO_2_PORT) with OIDC..."
mkdir -p "$ARMADILLO_2_DATA"
java -jar "$ARMADILLO_JAR" \
  --server.port=$ARMADILLO_2_PORT \
  --storage.root-dir="$ARMADILLO_2_DATA" \
  --spring.security.user.name=$ADMIN_USER \
  --spring.security.user.password=$ADMIN_PASS \
  --armadillo.docker-management-enabled=true \
  --spring.security.oauth2.client.provider.molgenis.issuer-uri="$OIDC_ISSUER_URI" \
  --spring.security.oauth2.client.registration.molgenis.client-id="$OIDC_CLIENT_ID" \
  --spring.security.oauth2.client.registration.molgenis.client-secret="$OIDC_CLIENT_SECRET" \
  --spring.security.oauth2.client.registration.molgenis.authorization-grant-type=authorization_code \
  --spring.security.oauth2.resourceserver.jwt.issuer-uri="$OIDC_ISSUER_URI" \
  --spring.security.oauth2.resourceserver.opaquetoken.client-id="$OIDC_CLIENT_ID" \
  > "$SCRIPT_DIR/armadillo2.log" 2>&1 &
ARMADILLO_2_PID=$!

wait_for_armadillo $ARMADILLO_1_PORT
wait_for_armadillo $ARMADILLO_2_PORT

# Verify auth info is available
log "Verifying OIDC auth info..."
AUTH_INFO=$(curl -sf "http://localhost:$ARMADILLO_1_PORT/actuator/info" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['auth']['issuerUri'])" 2>/dev/null || echo "")
if [ -z "$AUTH_INFO" ]; then
  fail "Armadillo /actuator/info did not return auth info. OIDC may not be configured correctly."
fi
log "OIDC configured: issuer=$AUTH_INFO"

# --- Step 4: Authenticate via device flow ------------------------------------

log "Creating flower-nodes.yaml..."
cat > "$NODES_CONFIG" <<EOF
nodes:
  node1:
    url: http://localhost:$ARMADILLO_1_PORT
  node2:
    url: http://localhost:$ARMADILLO_2_PORT
EOF

log ""
log "========================================="
log "  Authenticating to Armadillo nodes"
log "  A browser will open for each node."
log "========================================="
log ""

molgenis-flwr-authenticate --config "$NODES_CONFIG"

# Verify tokens were saved
[ -f "$TOKEN_FILE" ] || fail "Token file not found at $TOKEN_FILE. Authentication may have failed."

# Read tokens and URLs from the JSON file
log "Reading tokens from $TOKEN_FILE..."
TOKEN_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d.get('token-node1',''))")
TOKEN_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d.get('token-node2',''))")
URL_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d.get('url-node1',''))")
URL_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d.get('url-node2',''))")

[ -n "$TOKEN_NODE1" ] || fail "token-node1 is empty"
[ -n "$TOKEN_NODE2" ] || fail "token-node2 is empty"
[ -n "$URL_NODE1" ] || fail "url-node1 is empty"
[ -n "$URL_NODE2" ] || fail "url-node2 is empty"

log "Tokens and URLs loaded for node1 and node2."

# --- Step 5: Upload test data to Armadillo storage ----------------------------

# Admin uploads data using basic auth (separate from OIDC tokens used by FL)
log "Uploading test data to Armadillo storage..."

create_project $ARMADILLO_1_PORT "$PROJECT_NAME"
upload_to_storage $ARMADILLO_1_PORT "$PROJECT_NAME" "data/cifar10_train.pt" "$SCRIPT_DIR/cifar10_train.pt"
upload_to_storage $ARMADILLO_1_PORT "$PROJECT_NAME" "data/cifar10_test.pt" "$SCRIPT_DIR/cifar10_test.pt"

create_project $ARMADILLO_2_PORT "$PROJECT_NAME"
upload_to_storage $ARMADILLO_2_PORT "$PROJECT_NAME" "data/cifar10_train.pt" "$SCRIPT_DIR/cifar10_train.pt"
upload_to_storage $ARMADILLO_2_PORT "$PROJECT_NAME" "data/cifar10_test.pt" "$SCRIPT_DIR/cifar10_test.pt"

log "Test data uploaded to both Armadillo instances."

# --- Step 6: Create flower container configs via API --------------------------

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

# --- Step 7: Start containers via API ----------------------------------------

start_container $ARMADILLO_1_PORT "$SUPERNODE_1"
wait_for_container_running "$SUPERNODE_1"

start_container $ARMADILLO_2_PORT "$SUPERNODE_2"
wait_for_container_running "$SUPERNODE_2"

sleep 3

start_container $ARMADILLO_1_PORT "$CLIENTAPP_1"
wait_for_container_running "$CLIENTAPP_1"

start_container $ARMADILLO_2_PORT "$CLIENTAPP_2"
wait_for_container_running "$CLIENTAPP_2"

log "All flower containers started."

# --- Step 8: Start serverapp superexec ----------------------------------------

log "Starting serverapp superexec..."
docker run -d --rm \
  --name "$SERVERAPP" \
  "$SUPEREXEC_IMAGE" \
  --insecure \
  --plugin-type serverapp \
  --appio-api-address host.docker.internal:9091

wait_for_container_running "$SERVERAPP"

# --- Step 9: Run FL job with OIDC tokens and URLs ----------------------------

# The URLs need to be rewritten for access from inside Docker containers
# (the Python ClientApp runs inside Docker, not on the host)
DOCKER_URL_NODE1="http://host.docker.internal:$ARMADILLO_1_PORT"
DOCKER_URL_NODE2="http://host.docker.internal:$ARMADILLO_2_PORT"

log "Running FL job via 'flwr run --stream' with OIDC tokens..."
log "App directory: $FLWR_APP_DIR"

RUN_CONFIG="token-node1='$TOKEN_NODE1' url-node1='$DOCKER_URL_NODE1' token-node2='$TOKEN_NODE2' url-node2='$DOCKER_URL_NODE2'"

(cd "$FLWR_APP_DIR" && flwr run . local-deployment --stream --run-config "$RUN_CONFIG" 2>&1 | tee "$SCRIPT_DIR/flwr-run.log")
FLWR_EXIT=${PIPESTATUS[0]}

if [ "$FLWR_EXIT" -ne 0 ]; then
  log "flwr run exited with code $FLWR_EXIT"
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
  fail "FL job failed. See logs above."
fi

log "flwr run completed."

# --- Step 10: Wait for training to finish ------------------------------------

log "Waiting for training to complete..."
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

# --- Step 11: Verify ---------------------------------------------------------

log "Checking container states..."
for c in $SUPERNODE_1 $SUPERNODE_2 $CLIENTAPP_1 $CLIENTAPP_2; do
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
log "  Flower data loading test PASSED"
log "========================================="
log ""
log "Container states:"
docker ps --filter "name=flower-" --format "table {{.Names}}\t{{.Status}}\t{{.Image}}"
