#!/usr/bin/env bash
#
# Scenario D: app whose FAB hash is not on the whitelist (should be rejected)
#
# Pushes a freshly-built FAB of the same app straight to the SuperLink
# instead of pulling the Hub-served one. FAB builds aren't byte-deterministic
# (timestamps, archive ordering), so even identical source produces a
# different hash from the one register-flower-containers.sh approved — the
# SuperExec plugin's launch_task rejects it via PushTaskOutput before any app
# code runs. Check the clientapp container logs to confirm (NOT the supernode
# — the whitelist check happens at the SuperExec, not the SuperNode).
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -d "$FLWR_APP_DIR" ] || fail "App dir not found: $FLWR_APP_DIR"

write_flwr_cli_config

log "Scenario D: freshly-built (unwhitelisted) FAB pushed directly"
log "Expected: SuperExec rejects the run — check clientapp container logs for"
log "'is not on the approved whitelist'"
log ""

flwr run "$FLWR_APP_DIR" local --stream || true

log ""
log "--- $CLIENTAPP_1 logs ---"
docker logs "$CLIENTAPP_1" 2>&1 | tail -30
