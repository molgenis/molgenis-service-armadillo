#!/usr/bin/env bash
#
# Step 2: Prepare CIFAR10 test data and upload to both Armadillo instances.
#
# Uses basic auth (admin/admin) for project creation and data upload.
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

# --- Preflight ---------------------------------------------------------------

log "Checking prerequisites..."
command -v python3 >/dev/null 2>&1 || fail "python3 not found."
curl -sf "$ARMADILLO_1_URL/actuator/health" >/dev/null 2>&1 || fail "Armadillo 1 not reachable on port $ARMADILLO_1_PORT"
curl -sf "$ARMADILLO_2_URL/actuator/health" >/dev/null 2>&1 || fail "Armadillo 2 not reachable on port $ARMADILLO_2_PORT"

# --- Prepare test data -------------------------------------------------------

if [ -f "$SCRIPT_DIR/cifar10_train.pt" ] && [ -f "$SCRIPT_DIR/cifar10_test.pt" ]; then
  log "CIFAR10 test data already exists, skipping split_data.py."
else
  log "Preparing CIFAR10 test data..."
  (cd "$SCRIPT_DIR" && python3 "$FLWR_APP_DIR/pytorchexample/split_data.py")
  [ -f "$SCRIPT_DIR/cifar10_train.pt" ] || fail "split_data.py did not create cifar10_train.pt"
  [ -f "$SCRIPT_DIR/cifar10_test.pt" ] || fail "split_data.py did not create cifar10_test.pt"
  log "Test data prepared."
fi

# --- Upload to both Armadillo instances --------------------------------------

log "Uploading test data to Armadillo storage..."

for port in $ARMADILLO_1_PORT $ARMADILLO_2_PORT; do
  create_project $port "$PROJECT_NAME"
  grant_access $port "$RESEARCHER_EMAIL" "$PROJECT_NAME"

  # Verify the permission was stored
  project_info=$(curl -sf -u "$ADMIN_USER:$ADMIN_PASS" \
    "http://localhost:$port/access/projects/$PROJECT_NAME" 2>/dev/null || echo "FAILED")
  log "Project info on port $port: $project_info"
  if echo "$project_info" | grep -q "$RESEARCHER_EMAIL"; then
    log "Permission verified for $RESEARCHER_EMAIL on port $port."
  else
    fail "Permission for $RESEARCHER_EMAIL NOT found on port $port. Check Armadillo logs."
  fi

  upload_to_storage $port "$PROJECT_NAME" "data/cifar10_train.pt" "$SCRIPT_DIR/cifar10_train.pt"
  upload_to_storage $port "$PROJECT_NAME" "data/cifar10_test.pt" "$SCRIPT_DIR/cifar10_test.pt"
done

log ""
log "Test data uploaded to both Armadillo instances."
