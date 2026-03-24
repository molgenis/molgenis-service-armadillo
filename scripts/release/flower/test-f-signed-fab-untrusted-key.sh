#!/usr/bin/env bash
#
# Scenario F: Signed FAB with untrusted key (should be rejected by supernodes)
#
# Generates a throwaway keypair, signs the FAB with it, and submits.
# The key is NOT in trusted-entities.yaml, so the verified supernodes
# should reject the FAB even though it has a valid signature.
#
# Check supernode logs: docker logs flower-supernode-1
# Expected: FAB rejected — key not trusted
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

# Generate a throwaway keypair (not in trusted-entities.yaml)
UNTRUSTED_KEY="/tmp/untrusted-key"
log "Generating untrusted keypair at $UNTRUSTED_KEY..."
molgenis-flwr-keygen --name "$UNTRUSTED_KEY"

# Sign the FAB with the untrusted key
UNTRUSTED_FAB="/tmp/untrusted-study.sfab"
log "Signing FAB with untrusted key..."
molgenis-flwr-sign \
  --app-dir "$FLWR_APP_DIR" \
  --private-key "${UNTRUSTED_KEY}.key" \
  --output "$UNTRUSTED_FAB"

log "Scenario F: Signed FAB with untrusted key"
log "Expected: supernodes reject the FAB (key not in trusted-entities.yaml)"
log "Check logs: docker logs flower-supernode-1"
log ""

molgenis-flwr-run \
  --signed-fab "$UNTRUSTED_FAB" \
  --federation-address 127.0.0.1:9093 \
  --stream \
  --run-config "token-node1='$TOKEN_NODE1' url-node1='$DOCKER_URL_NODE1' token-node2='$TOKEN_NODE2' url-node2='$DOCKER_URL_NODE2'"