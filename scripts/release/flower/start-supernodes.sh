#!/usr/bin/env bash
#
# Start the verified supernodes and clientapps via the Armadillo API.
#
# The supernode/clientapp container configs must already be registered
# (PUT /containers) — that happens in test-flower-containers.sh.
#
# /tmp/trusted-entities.yaml must exist on the host before running this,
# because the supernode containers bind-mount it.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f /tmp/trusted-entities.yaml ] || fail "/tmp/trusted-entities.yaml not found. Generate it before starting supernodes."

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