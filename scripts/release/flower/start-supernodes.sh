#!/usr/bin/env bash
#
# Start the supernodes and clientapps via the Armadillo API.
#
# The supernode/clientapp container configs must already be registered
# (PUT /containers) — that happens in test-flower-containers.sh.
# Armadillo mounts trusted-entities.yaml, ca.crt and the supernode auth
# key from each node's data/system/flower directory; run
# generate_flower_credentials first to create them.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -s "$ARMADILLO_1_FLOWER_DIR/trusted-entities.yaml" ] || fail "$ARMADILLO_1_FLOWER_DIR/trusted-entities.yaml missing. Run generate_flower_credentials first."

# Make sure the supernode/clientapp configs are registered (idempotent PUT).
"$SCRIPT_DIR/register-flower-containers.sh"

start_container $ARMADILLO_1_PORT "$SUPERNODE_1"
wait_for_container_running "$SUPERNODE_1"
start_container $ARMADILLO_2_PORT "$SUPERNODE_2"
wait_for_container_running "$SUPERNODE_2"

sleep 3

start_container $ARMADILLO_1_PORT "$CLIENTAPP_1"
wait_for_container_running "$CLIENTAPP_1"
start_container $ARMADILLO_2_PORT "$CLIENTAPP_2"
wait_for_container_running "$CLIENTAPP_2"