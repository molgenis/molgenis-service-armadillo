#!/usr/bin/env bash
#
# Register the supernode and clientapp container configs on both
# Armadillo nodes via PUT /containers. Does NOT start them.
#
# Armadillo auto-appends the supernode security args (--root-certificates,
# --auth-supernode-private-key, --clientappio-api-address, --isolation
# process) and mounts the files given by caCertPath/authPrivateKeyPath.
# For the superexec containers, Armadillo derives the mounted FAB whitelist
# file from the fabWhitelist array below and appends --fab-whitelist. Run
# generate_flower_credentials first.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

log "Fetching and approving $HUB_APP==$HUB_APP_VERSION for the whitelist..."
FAB_FILE="$(fetch_fab "$HUB_APP" "$HUB_APP_VERSION" "$SCRIPT_DIR/.fabs")"
FAB_ENTRY_JSON="$(approve_fab_entry_json "$FAB_FILE")"
log "Approved: $FAB_ENTRY_JSON"

log "Registering supernode + clientapp configs on Armadillo 1..."

put_container $ARMADILLO_1_PORT "$(cat <<EOF
{
  "type": "flower-supernode",
  "name": "$SUPERNODE_1",
  "image": "$SUPERNODE_IMAGE",
  "caCertPath": "$ARMADILLO_1_FLOWER_DIR/ca.crt",
  "authPrivateKeyPath": "$ARMADILLO_1_FLOWER_DIR/credentials",
  "dockerArgs": [
    "--superlink", "host.docker.internal:9092"
  ]
}
EOF
)"

put_container $ARMADILLO_1_PORT "$(cat <<EOF
{
  "type": "flower-superexec",
  "name": "$CLIENTAPP_1",
  "image": "$SUPEREXEC_IMAGE",
  "fabWhitelistPath": "$ARMADILLO_1_FLOWER_DIR/$CLIENTAPP_1-fab-whitelist.yaml",
  "fabWhitelist": [$FAB_ENTRY_JSON],
  "dockerArgs": [
    "--insecure",
    "--appio-api-address", "$SUPERNODE_1:9094"
  ]
}
EOF
)"

log "Registering supernode + clientapp configs on Armadillo 2..."

put_container $ARMADILLO_2_PORT "$(cat <<EOF
{
  "type": "flower-supernode",
  "name": "$SUPERNODE_2",
  "image": "$SUPERNODE_IMAGE",
  "caCertPath": "$ARMADILLO_2_FLOWER_DIR/ca.crt",
  "authPrivateKeyPath": "$ARMADILLO_2_FLOWER_DIR/credentials",
  "dockerArgs": [
    "--superlink", "host.docker.internal:9092"
  ]
}
EOF
)"

put_container $ARMADILLO_2_PORT "$(cat <<EOF
{
  "type": "flower-superexec",
  "name": "$CLIENTAPP_2",
  "image": "$SUPEREXEC_IMAGE",
  "fabWhitelistPath": "$ARMADILLO_2_FLOWER_DIR/$CLIENTAPP_2-fab-whitelist.yaml",
  "fabWhitelist": [$FAB_ENTRY_JSON],
  "dockerArgs": [
    "--insecure",
    "--appio-api-address", "$SUPERNODE_2:9094"
  ]
}
EOF
)"

log "Supernode and clientapp configs registered."
