#!/usr/bin/env bash
#
# Scenario A: Hub app, whitelisted hash, correct tokens (should succeed)
#
# Full end-to-end: the app is pulled from Flower Hub by the SuperLink; its FAB
# hash matches what register-flower-containers.sh approved into each
# clientapp's whitelist, so the SuperExec plugin allows the task to launch;
# valid tokens authenticate with Armadillo; data loads via push-data; training
# completes.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f "$TOKEN_FILE" ] || fail "Token file not found at $TOKEN_FILE. Run armadillo-flwr-authenticate first."

write_flwr_cli_config
RUN_CONFIG="$(run_config_from_tokens)"

log "Scenario A: Hub app, whitelisted hash, correct tokens"
log "Expected: training completes successfully"
log ""

flwr run "$HUB_APP==$HUB_APP_VERSION" local --stream --run-config "$RUN_CONFIG"
