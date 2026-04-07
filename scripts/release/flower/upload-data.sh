#!/usr/bin/env bash
#
# Create the project on both Armadillo nodes and upload the CIFAR10 test data.
# Requires the Armadillos to be running and the test data files to exist
# (run prepare-test-data.sh first).
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f "$SCRIPT_DIR/cifar10_train.pt" ] || fail "cifar10_train.pt not found. Run prepare-test-data.sh first."
[ -f "$SCRIPT_DIR/cifar10_test.pt" ]  || fail "cifar10_test.pt not found. Run prepare-test-data.sh first."

log "Uploading test data to Armadillo storage..."

create_project $ARMADILLO_1_PORT "$PROJECT_NAME"
upload_to_storage $ARMADILLO_1_PORT "$PROJECT_NAME" "data/cifar10_train.pt" "$SCRIPT_DIR/cifar10_train.pt"
upload_to_storage $ARMADILLO_1_PORT "$PROJECT_NAME" "data/cifar10_test.pt"  "$SCRIPT_DIR/cifar10_test.pt"

create_project $ARMADILLO_2_PORT "$PROJECT_NAME"
upload_to_storage $ARMADILLO_2_PORT "$PROJECT_NAME" "data/cifar10_train.pt" "$SCRIPT_DIR/cifar10_train.pt"
upload_to_storage $ARMADILLO_2_PORT "$PROJECT_NAME" "data/cifar10_test.pt"  "$SCRIPT_DIR/cifar10_test.pt"

log "Test data uploaded."