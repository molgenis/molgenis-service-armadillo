#!/usr/bin/env bash
#
# Scenario A: Signed FAB + correct tokens (should succeed)
#
# Full end-to-end: signed FAB passes verification, valid tokens authenticate
# with Armadillo, data loads from storage, training completes.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f "$TOKEN_FILE" ] || fail "Token file not found at $TOKEN_FILE. Run test-flower-containers.sh first."
[ -f /tmp/study.sfab ] || fail "Signed FAB not found at /tmp/study.sfab. See README Step 4."

TOKEN_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['token-node1'])")
TOKEN_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['token-node2'])")
URL_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['url-node1'])")
URL_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['url-node2'])")

DOCKER_URL_NODE1="${URL_NODE1/localhost/host.docker.internal}"
DOCKER_URL_NODE2="${URL_NODE2/localhost/host.docker.internal}"

log "Scenario A: Signed FAB + correct tokens"
log "Expected: training completes successfully"
log ""

molgenis-flwr-run \
  --signed-fab /tmp/study.sfab \
  --federation-address 127.0.0.1:9093 \
  --run-config "token-node1='$TOKEN_NODE1' url-node1='$DOCKER_URL_NODE1' token-node2='$TOKEN_NODE2' url-node2='$DOCKER_URL_NODE2'"
