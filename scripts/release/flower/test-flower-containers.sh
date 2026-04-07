#!/usr/bin/env bash
#
# Start Flower + Armadillo infrastructure for manual e2e testing.
#
# Sets up:
#   - Two Armadillo instances with OIDC (ports 8080/8081)
#   - Flower superlink (ports 9091-9093)
#   - Verified supernode + clientapp configs registered via Armadillo API
#     (started later by the demo, after trusted-entities.yaml is created)
#   - Serverapp superexec
#   - Test data uploaded to Armadillo storage
#
# Then waits for you to submit FABs manually to test scenarios.
#
# Prerequisites:
#   - Docker running
#   - Armadillo bootJar built: ./gradlew bootJar
#   - Java 17+
#   - timmyjc/verified-supernode:test pushed (see README Step 3)
#   - timmyjc/superexec-data-test:0.0.1 pushed
#   - Python with torch + torchvision (for test data generation)
#
# Usage:
#   ./scripts/release/flower/test-flower-containers.sh
#
# Cleanup:
#   ./scripts/release/flower/cleanup.sh (or Ctrl+C)
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

# --- Cleanup on exit ---------------------------------------------------------

on_exit() {
  "$SCRIPT_DIR/cleanup.sh"
}
trap on_exit EXIT

# --- Bring everything up -----------------------------------------------------

"$SCRIPT_DIR/prepare-test-data.sh"
"$SCRIPT_DIR/start-superlink.sh"
"$SCRIPT_DIR/start-armadillos.sh"
"$SCRIPT_DIR/upload-data.sh"
"$SCRIPT_DIR/grant-access.sh"
"$SCRIPT_DIR/register-flower-containers.sh"

log "Supernode and clientapp configs registered but not started."
log "Create /tmp/trusted-entities.yaml, then run start-supernodes.sh."

# --- Ready -------------------------------------------------------------------

log ""
log "========================================="
log "  Infrastructure ready"
log "========================================="
log ""
log "  Armadillo 1:  http://localhost:$ARMADILLO_1_PORT"
log "  Armadillo 2:  http://localhost:$ARMADILLO_2_PORT"
log "  SuperLink:    127.0.0.1:9093"
log "  Project:      $PROJECT_NAME"
log ""
log "  Open a new terminal and run the test scenarios:"
log ""
log "    ./scripts/release/flower/test-a-signed-fab-correct-tokens.sh"
log "    ./scripts/release/flower/test-b-signed-fab-wrong-token.sh"
log "    ./scripts/release/flower/test-c-signed-fab-wrong-project.sh"
log "    ./scripts/release/flower/test-d-unsigned-fab.sh"
log "    ./scripts/release/flower/test-e-signed-fab-no-tokens.sh"
log ""
log "  Cleanup:      Ctrl+C"
log ""
log "  Tailing all logs below..."
log ""

# Tail Armadillo logs
tail -f "$SCRIPT_DIR/armadillo1.log" 2>/dev/null | sed "s/^/[armadillo-1] /" &
tail -f "$SCRIPT_DIR/armadillo2.log" 2>/dev/null | sed "s/^/[armadillo-2] /" &

# Tail Docker container logs
for c in "$SUPERLINK" "$SUPERNODE_1" "$SUPERNODE_2" "$CLIENTAPP_1" "$CLIENTAPP_2" "$SERVERAPP"; do
  if docker inspect "$c" >/dev/null 2>&1; then
    docker logs --tail 20 -f "$c" 2>&1 | sed "s/^/[$c] /" &
  fi
done

wait
