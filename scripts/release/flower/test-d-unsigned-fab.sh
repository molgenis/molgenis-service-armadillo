#!/usr/bin/env bash
#
# Scenario D: Unsigned FAB (should be rejected by supernodes)
#
# Submits an unsigned FAB via flwr run. The verified supernodes should
# reject it because the signature verification fails.
#
# Check supernode logs: docker logs flower-supernode-1
# Expected: "REJECTED FAB: signature verification failed"
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f "$TOKEN_FILE" ] || fail "Token file not found at $TOKEN_FILE. Run test-flower-containers.sh first."

TOKEN_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['token-node1'])")
TOKEN_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['token-node2'])")
URL_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['url-node1'])")
URL_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['url-node2'])")

DOCKER_URL_NODE1="${URL_NODE1/localhost/host.docker.internal}"
DOCKER_URL_NODE2="${URL_NODE2/localhost/host.docker.internal}"

log "Scenario D: Unsigned FAB"
log "Expected: supernodes reject the FAB (signature verification failed)"
log "Check logs: docker logs flower-supernode-1"
log ""

cd "$FLWR_APP_DIR"
flwr run . local-deployment --stream \
  --run-config "token-node1='$TOKEN_NODE1' url-node1='$DOCKER_URL_NODE1' token-node2='$TOKEN_NODE2' url-node2='$DOCKER_URL_NODE2'"
