#!/usr/bin/env bash
#
# Scenario F: previously-approved hash removed from the whitelist
# (should be rejected)
#
# Swaps each clientapp's fab-whitelist.yaml for one with an unrelated
# (bogus) entry, restarts the clientapp containers so the plugin re-reads it
# at startup, then runs the same app that Scenario A approved. The SuperExec
# plugin now finds no matching fab_hash and rejects via PushTaskOutput.
# Restores the original whitelist afterwards.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

write_flwr_cli_config

WHITELIST_1="$ARMADILLO_1_FLOWER_DIR/$CLIENTAPP_1-fab-whitelist.yaml"
WHITELIST_2="$ARMADILLO_2_FLOWER_DIR/$CLIENTAPP_2-fab-whitelist.yaml"

restore() {
  for f in "$WHITELIST_1" "$WHITELIST_2"; do
    if [ -f "$f.bak" ]; then
      mv "$f.bak" "$f"
    fi
  done
  docker restart "$CLIENTAPP_1" "$CLIENTAPP_2" >/dev/null
  log "Restored the original whitelist on both clientapp containers."
}
trap restore EXIT

for f in "$WHITELIST_1" "$WHITELIST_2"; do
  cp "$f" "$f.bak"
  cat > "$f" <<'EOF'
- fab_id: '@nobody/unrelated-app'
  fab_version: '0.0.1'
  fab_hash: '0000000000000000000000000000000000000000000000000000000000000000'
EOF
done

log "Restarting clientapp containers with an unrelated-only whitelist..."
docker restart "$CLIENTAPP_1" "$CLIENTAPP_2" >/dev/null
sleep 3

log "Scenario F: previously-approved app, now removed from the whitelist"
log "Expected: SuperExec rejects the run — check clientapp container logs for"
log "'is not on the approved whitelist'"
log ""

flwr run "$HUB_APP==$HUB_APP_VERSION" local --stream || true

log ""
log "--- $CLIENTAPP_1 logs ---"
docker logs "$CLIENTAPP_1" 2>&1 | tail -30
