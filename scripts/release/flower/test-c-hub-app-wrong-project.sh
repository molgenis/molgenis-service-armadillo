#!/usr/bin/env bash
#
# Scenario C: Hub app + wrong project (should fail auth)
#
# Valid tokens but requesting data from a project the user does not have
# access to.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f "$TOKEN_FILE" ] || fail "Token file not found at $TOKEN_FILE. Run armadillo-flwr-authenticate first."

write_flwr_cli_config
RUN_CONFIG="$(run_config_from_tokens)"

log "Scenario C: Hub app + wrong project"
log "Expected: data loading fails with HTTP 403/404 from Armadillo"
log ""

flwr run "$HUB_APP==$HUB_APP_VERSION" local --stream --run-config "$RUN_CONFIG project='no-such-project'"
