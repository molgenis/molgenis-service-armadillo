#!/usr/bin/env bash
#
# Start Flower + Armadillo infrastructure for manual e2e testing.
#
# Sets up:
#   - Two Armadillo instances with OIDC (ports 8080/8081)
#   - Flower superlink (ports 9091-9093)
#   - Verified supernodes + clientapps via Armadillo API
#   - Serverapp superexec
#   - Test data uploaded to Armadillo storage
#   - OIDC authentication (opens browser)
#
# Then waits for you to submit FABs manually to test scenarios.
#
# Prerequisites:
#   - Docker running
#   - Armadillo bootJar built: ./gradlew bootJar
#   - Java 17+
#   - molgenis-flwr-armadillo installed (pip install -e ../molgenis-flwr-armadillo)
#   - timmyjc/verified-supernode:test pushed (see README Step 3)
#   - timmyjc/superexec-data-test:0.0.1 pushed
#   - Python with torch + torchvision (for test data generation)
#
# Usage:
#   ./scripts/release/flower/test-flower-containers.sh
#
# Cleanup:
#   ./scripts/release/flower/cleanup.sh (or Ctrl+C)
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

VERIFIED_SUPERNODE_IMAGE="${VERIFIED_SUPERNODE_IMAGE:-timmyjc/verified-supernode:test}"

# --- Preflight checks --------------------------------------------------------

log "Checking prerequisites..."

[ -f "$ARMADILLO_JAR" ] || fail "Armadillo JAR not found at $ARMADILLO_JAR. Run: ./gradlew bootJar"
docker info >/dev/null 2>&1 || fail "Docker is not running."
command -v python3 >/dev/null 2>&1 || fail "python3 not found."
command -v molgenis-flwr-authenticate >/dev/null 2>&1 || \
  fail "molgenis-flwr-authenticate not found. Install: pip install -e ../molgenis-flwr-armadillo"
[ -d "$FLWR_APP_DIR" ] || fail "Flower app directory not found at $FLWR_APP_DIR"

# --- Cleanup on exit ---------------------------------------------------------

on_exit() {
  "$SCRIPT_DIR/cleanup.sh"
}
trap on_exit EXIT

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

echo "$ARMADILLO_1_PID $ARMADILLO_2_PID" > "$PID_FILE"

wait_for_armadillo $ARMADILLO_1_PORT
wait_for_armadillo $ARMADILLO_2_PORT

# Verify OIDC is configured
AUTH_INFO=$(curl -sf "http://localhost:$ARMADILLO_1_PORT/actuator/info" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['auth']['issuerUri'])" 2>/dev/null || echo "")
[ -n "$AUTH_INFO" ] || fail "Armadillo OIDC not configured. Check armadillo1.log."
log "OIDC configured: issuer=$AUTH_INFO"

# --- Step 4: Upload test data ------------------------------------------------

log "Uploading test data to Armadillo storage..."

create_project $ARMADILLO_1_PORT "$PROJECT_NAME"
upload_to_storage $ARMADILLO_1_PORT "$PROJECT_NAME" "data/cifar10_train.pt" "$SCRIPT_DIR/cifar10_train.pt"
upload_to_storage $ARMADILLO_1_PORT "$PROJECT_NAME" "data/cifar10_test.pt" "$SCRIPT_DIR/cifar10_test.pt"

create_project $ARMADILLO_2_PORT "$PROJECT_NAME"
upload_to_storage $ARMADILLO_2_PORT "$PROJECT_NAME" "data/cifar10_train.pt" "$SCRIPT_DIR/cifar10_train.pt"
upload_to_storage $ARMADILLO_2_PORT "$PROJECT_NAME" "data/cifar10_test.pt" "$SCRIPT_DIR/cifar10_test.pt"

log "Test data uploaded."

# --- Step 5: Authenticate via OIDC ------------------------------------------

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

[ -f "$TOKEN_FILE" ] || fail "Token file not found at $TOKEN_FILE"

# Read tokens and URLs
TOKEN_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d.get('token-node1',''))")
TOKEN_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d.get('token-node2',''))")
URL_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d.get('url-node1',''))")
URL_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d.get('url-node2',''))")

[ -n "$TOKEN_NODE1" ] || fail "token-node1 is empty"
[ -n "$TOKEN_NODE2" ] || fail "token-node2 is empty"

log "Tokens loaded for node1 and node2."

# Rewrite URLs for Docker container access
DOCKER_URL_NODE1="http://host.docker.internal:$ARMADILLO_1_PORT"
DOCKER_URL_NODE2="http://host.docker.internal:$ARMADILLO_2_PORT"

# --- Step 6: Create and start flower containers ------------------------------

log "Creating verified supernode configs on Armadillo 1..."

put_container $ARMADILLO_1_PORT "$(cat <<EOF
{
  "type": "flower-supernode",
  "name": "$SUPERNODE_1",
  "image": "$VERIFIED_SUPERNODE_IMAGE",
  "dockerArgs": [
    "--trusted-entities", "/app/trusted-entities.yaml",
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=0 num-partitions=2 node-name='node1'",
    "--clientappio-api-address", "0.0.0.0:9094",
    "--isolation", "process"
  ]
}
EOF
)"

put_container $ARMADILLO_1_PORT "$(cat <<EOF
{
  "type": "flower-superexec",
  "name": "$CLIENTAPP_1",
  "image": "$SUPEREXEC_IMAGE",
  "dockerArgs": [
    "--insecure",
    "--plugin-type", "clientapp",
    "--appio-api-address", "$SUPERNODE_1:9094"
  ]
}
EOF
)"

log "Creating verified supernode configs on Armadillo 2..."

put_container $ARMADILLO_2_PORT "$(cat <<EOF
{
  "type": "flower-supernode",
  "name": "$SUPERNODE_2",
  "image": "$VERIFIED_SUPERNODE_IMAGE",
  "dockerArgs": [
    "--trusted-entities", "/app/trusted-entities.yaml",
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=1 num-partitions=2 node-name='node2'",
    "--clientappio-api-address", "0.0.0.0:9095",
    "--isolation", "process"
  ]
}
EOF
)"

put_container $ARMADILLO_2_PORT "$(cat <<EOF
{
  "type": "flower-superexec",
  "name": "$CLIENTAPP_2",
  "image": "$SUPEREXEC_IMAGE",
  "dockerArgs": [
    "--insecure",
    "--plugin-type", "clientapp",
    "--appio-api-address", "$SUPERNODE_2:9095"
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

log "Starting serverapp superexec..."
docker run -d --rm \
  --name "$SERVERAPP" \
  "$SUPEREXEC_IMAGE" \
  --insecure \
  --plugin-type serverapp \
  --appio-api-address host.docker.internal:9091

wait_for_container_running "$SERVERAPP"

# --- Ready -------------------------------------------------------------------

log ""
log "========================================="
log "  Infrastructure ready"
log "========================================="
log ""
log "  Armadillo 1:  http://localhost:$ARMADILLO_1_PORT"
log "  Armadillo 2:  http://localhost:$ARMADILLO_2_PORT"
log "  SuperLink:    127.0.0.1:9093"
log "  Project:      $PROJECT_NAME"
log "  Token file:   $TOKEN_FILE"
log ""
log "  Open a new terminal and run the test scenarios:"
log ""
log "    ./scripts/release/flower/test-a-signed-fab-correct-tokens.sh"
log "    ./scripts/release/flower/test-b-signed-fab-wrong-token.sh"
log "    ./scripts/release/flower/test-c-signed-fab-wrong-project.sh"
log "    ./scripts/release/flower/test-d-unsigned-fab.sh"
log "    ./scripts/release/flower/test-e-signed-fab-no-tokens.sh"
log ""
log "  View logs:    ./scripts/release/flower/logs.sh"
log "  Cleanup:      ./scripts/release/flower/cleanup.sh (or Ctrl+C)"
log ""

# Wait until interrupted
wait
