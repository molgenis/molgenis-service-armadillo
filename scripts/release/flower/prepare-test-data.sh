#!/usr/bin/env bash
#
# Generate the CIFAR10 test data files used by the Flower demo.
# Produces $SCRIPT_DIR/cifar10_train.pt and $SCRIPT_DIR/cifar10_test.pt.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

command -v python3 >/dev/null 2>&1 || fail "python3 not found."
[ -d "$FLWR_APP_DIR" ] || fail "Flower app directory not found at $FLWR_APP_DIR"

log "Preparing CIFAR10 test data..."
(cd "$SCRIPT_DIR" && python3 "$FLWR_APP_DIR/pytorchexample/split_data.py")
[ -f "$SCRIPT_DIR/cifar10_train.pt" ] || fail "split_data.py did not create cifar10_train.pt"
[ -f "$SCRIPT_DIR/cifar10_test.pt" ] || fail "split_data.py did not create cifar10_test.pt"
log "Test data prepared."