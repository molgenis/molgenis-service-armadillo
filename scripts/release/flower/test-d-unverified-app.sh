#!/usr/bin/env bash
#
# Scenario D: app not from the Hub (should be rejected by supernodes)
#
# The FAB is built locally and pushed directly to the SuperLink. The stock
# SuperLink strips verifications from directly-submitted FABs, so the
# supernodes' trusted-entities check rejects the run with
# FAB_VERIFICATION_ERROR before any app code executes. Check the supernode
# logs to confirm.
#
# The same rejection applies to any Hub app whose signers are not listed in
# trusted-entities.yaml.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -d "$FLWR_APP_DIR" ] || fail "App dir not found: $FLWR_APP_DIR"

write_flwr_cli_config

log "Scenario D: locally-built app pushed directly (not via Hub)"
log "Expected: supernodes reject the run (FAB_VERIFICATION_ERROR in supernode logs)"
log ""

flwr run "$FLWR_APP_DIR" local --stream
