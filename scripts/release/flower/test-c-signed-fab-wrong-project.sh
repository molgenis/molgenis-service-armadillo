#!/usr/bin/env bash
#
# Scenario C: Signed FAB + wrong project (should fail auth)
#
# Valid tokens but requesting data from a project the user doesn't
# have access to.
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

log "Scenario C: Signed FAB + wrong project"
log "Expected: FAB accepted, tokens valid, but data loading fails (project not found)"
log ""

molgenis-flwr-run \
  --signed-fab /tmp/study.sfab \
  --federation-address 127.0.0.1:9093 \
  --run-config "token-node1='$TOKEN_NODE1' url-node1='$DOCKER_URL_NODE1' token-node2='$TOKEN_NODE2' url-node2='$DOCKER_URL_NODE2' project='nonexistent-project'"
