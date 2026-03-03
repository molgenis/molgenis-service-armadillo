#!/usr/bin/env bash
#
# Step 4: Authenticate via OIDC device flow and run the FL job.
#
# Opens a browser for each node to complete OIDC authentication,
# then runs `flwr run` with the obtained tokens and URLs.
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

# --- Preflight ---------------------------------------------------------------

log "Checking prerequisites..."
command -v flwr >/dev/null 2>&1 || fail "flwr CLI not found. Install with: pip install flwr"
command -v molgenis-flwr-authenticate >/dev/null 2>&1 || fail "molgenis-flwr-authenticate not found. Install with: pip install -e ../molgenis-flwr-armadillo"
[ -d "$FLWR_APP_DIR" ] || fail "Flower app directory not found at $FLWR_APP_DIR"

# --- Authenticate via device flow --------------------------------------------

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

# Read tokens and URLs
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

# --- Run FL job --------------------------------------------------------------

# Rewrite URLs for Docker-internal access (ClientApp runs inside Docker)
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

# --- Wait for training to finish ---------------------------------------------

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

# --- Verify ------------------------------------------------------------------

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
