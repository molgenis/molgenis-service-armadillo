#!/usr/bin/env bash
#
# Scenario E: Hub app + no tokens (should fail)
#
# The app's hash is whitelisted so the SuperExec launches it, but no tokens
# are provided, so data loading fails in the clientapp.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

write_flwr_cli_config

log "Scenario E: Hub app + no tokens"
log "Expected: clientapp fails with 'No token found for URL'"
log ""

flwr run "$HUB_APP==$HUB_APP_VERSION" local --stream
