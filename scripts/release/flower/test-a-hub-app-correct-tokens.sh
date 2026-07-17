#!/usr/bin/env bash
#
# Scenario A: Hub app + correct tokens (should succeed)
#
# Full end-to-end: the app is pulled from Flower Hub by the SuperLink, its
# reviewer signature passes the supernodes' trusted-entities check, valid
# tokens authenticate with Armadillo, data loads via push-data, training
# completes.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f "$TOKEN_FILE" ] || fail "Token file not found at $TOKEN_FILE. Run armadillo-flwr-authenticate first."

write_flwr_cli_config
RUN_CONFIG="$(run_config_from_tokens)"

log "Scenario A: Hub app + correct tokens"
log "Expected: training completes successfully"
log ""

flwr run "$HUB_APP" local --stream --run-config "$RUN_CONFIG"
