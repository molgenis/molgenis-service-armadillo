#!/usr/bin/env bash
#
# Scenario E: Signed FAB + no tokens (should fail)
#
# Signed FAB passes verification but no tokens are provided,
# so data loading fails when the clientapp tries to authenticate.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f /tmp/study.sfab ] || fail "Signed FAB not found at /tmp/study.sfab. See README Step 4."

log "Scenario E: Signed FAB + no tokens"
log "Expected: FAB accepted, but data loading fails (no tokens provided)"
log ""

molgenis-flwr-run \
  --signed-fab /tmp/study.sfab \
  --federation-address 127.0.0.1:9093
