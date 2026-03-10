#!/usr/bin/env bash
#
# Scenario B: Signed FAB + wrong token (should fail auth)
#
# FAB passes signature check but Armadillo rejects the invalid token
# when the clientapp tries to load data.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f "$TOKEN_FILE" ] || fail "Token file not found at $TOKEN_FILE. Run test-flower-containers.sh first."
[ -f /tmp/study.sfab ] || fail "Signed FAB not found at /tmp/study.sfab. See README Step 4."

URL_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['url-node1'])")
URL_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['url-node2'])")

DOCKER_URL_NODE1="${URL_NODE1/localhost/host.docker.internal}"
DOCKER_URL_NODE2="${URL_NODE2/localhost/host.docker.internal}"

log "Scenario B: Signed FAB + wrong token"
log "Expected: FAB accepted, but data loading fails with auth error"
log ""

molgenis-flwr-run \
  --signed-fab /tmp/study.sfab \
  --federation-address 127.0.0.1:9093 \
  --run-config "token-node1='INVALID_TOKEN' url-node1='$DOCKER_URL_NODE1' token-node2='INVALID_TOKEN' url-node2='$DOCKER_URL_NODE2'"
