#!/usr/bin/env bash
#
# Scenario F: Hub app signed by a reviewer the nodes do not trust
# (should be rejected by supernodes)
#
# The app on Flower Hub carries a valid reviewer signature, but the
# supernodes' trusted-entities.yaml is temporarily replaced with a
# different key, so _verify_fab finds no trusted signature and the run is
# rejected with FAB_VERIFICATION_ERROR. Check the supernode logs to
# confirm. The original trusted-entities.yaml is restored afterwards.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

write_flwr_cli_config

TMP_KEY="$(mktemp -d)/untrusted"
ssh-keygen -t ed25519 -q -N "" -f "$TMP_KEY"

restore() {
  for dir in "$ARMADILLO_1_FLOWER_DIR" "$ARMADILLO_2_FLOWER_DIR"; do
    if [ -f "$dir/trusted-entities.yaml.bak" ]; then
      mv "$dir/trusted-entities.yaml.bak" "$dir/trusted-entities.yaml"
    fi
  done
  docker restart "$SUPERNODE_1" "$SUPERNODE_2" >/dev/null
  log "Restored original trusted-entities.yaml on both nodes."
}
trap restore EXIT

for dir in "$ARMADILLO_1_FLOWER_DIR" "$ARMADILLO_2_FLOWER_DIR"; do
  cp "$dir/trusted-entities.yaml" "$dir/trusted-entities.yaml.bak"
  printf "untrusted-test-key: %s\n" "$(cat "$TMP_KEY.pub")" > "$dir/trusted-entities.yaml"
done

log "Restarting supernodes with untrusted-only trusted-entities.yaml..."
docker restart "$SUPERNODE_1" "$SUPERNODE_2" >/dev/null
sleep 5

log "Scenario F: Hub app signed by an untrusted reviewer"
log "Expected: supernodes reject the run (FAB_VERIFICATION_ERROR in supernode logs)"
log ""

flwr run "$HUB_APP" local --stream || true
