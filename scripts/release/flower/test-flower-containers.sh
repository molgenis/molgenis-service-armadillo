#!/usr/bin/env bash
#
# Start Flower + Armadillo infrastructure for manual e2e testing.
#
# Sets up:
#   - TLS certs and supernode auth keys
#   - Two Armadillo instances with OIDC (ports 8080/8081)
#   - Flower superlink with TLS (ports 9091-9093), stock image
#   - The Hub app's FAB fetched, hashed and approved into each clientapp's
#     FAB whitelist (see FAB_HASH_WHITELIST_PLAN.md)
#   - Supernode + clientapp configs registered via Armadillo API (clientapp
#     runs our whitelist-enforcing SuperExec image; supernode is stock)
#   - Serverapp superexec (same image, --entrypoint override back to stock
#     flower-superexec — ServerApp-side trust isn't in scope for the whitelist)
#   - Test data uploaded to Armadillo storage
#
# Then waits for you to run the Hub app test scenarios.
#
# Prerequisites:
#   - Docker running
#   - Armadillo bootJar built: ./gradlew bootJar
#   - Java 17+
#   - App published on Flower Hub as $HUB_APP==$HUB_APP_VERSION
#     (flwr app publish) — review/signing is NOT required, trust is now the
#     FAB-hash whitelist, not Hub's reviewer signature
#   - "127.0.0.1 host.docker.internal" in /etc/hosts
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

generate_flower_credentials
"$SCRIPT_DIR/prepare-test-data.sh"
"$SCRIPT_DIR/start-superlink.sh" &
sleep 3
"$SCRIPT_DIR/start-armadillos.sh"
"$SCRIPT_DIR/upload-data.sh"
"$SCRIPT_DIR/grant-access.sh"
"$SCRIPT_DIR/register-flower-containers.sh"
register_supernode_keys
"$SCRIPT_DIR/start-supernodes.sh"

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
log "  Authenticate, then open a new terminal and run the test scenarios:"
log ""
log "    armadillo-flwr-authenticate --config scripts/release/flower/flower-nodes.yaml"
log ""
log "    ./scripts/release/flower/test-a-hub-app-correct-tokens.sh"
log "    ./scripts/release/flower/test-b-hub-app-wrong-token.sh"
log "    ./scripts/release/flower/test-c-hub-app-wrong-project.sh"
log "    ./scripts/release/flower/test-d-unverified-app.sh"
log "    ./scripts/release/flower/test-e-hub-app-no-tokens.sh"
log "    ./scripts/release/flower/test-f-hub-app-untrusted-reviewer.sh"
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
