#!/usr/bin/env bash
#
# Generate the CIFAR10 test data files used by the Flower demo.
# Produces $SCRIPT_DIR/cifar10_train.pt and $SCRIPT_DIR/cifar10_test.pt.
#
# Extracts split_data.py from the Hub-fetched FAB rather than a local
# checkout of the app source — this only needs the app's data-prep script,
# not a working local dev environment for it, and matches how the app is
# actually distributed (register-flower-containers.sh fetches the same FAB
# again independently for approval; fetch-fab is deterministic for a pinned
# version, so the duplicate download isn't a correctness concern).
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

command -v python3 >/dev/null 2>&1 || fail "python3 not found."

log "Fetching $HUB_APP==$HUB_APP_VERSION to extract its data-prep script..."
FAB_FILE="$(fetch_fab "$HUB_APP" "$HUB_APP_VERSION" "$SCRIPT_DIR/.fabs")"

EXTRACT_DIR="$(mktemp -d)"
trap 'rm -rf "$EXTRACT_DIR"' EXIT
unzip -q -o "$FAB_FILE" -d "$EXTRACT_DIR"

SPLIT_DATA_SCRIPT="$(find "$EXTRACT_DIR" -name split_data.py | head -1)"
[ -n "$SPLIT_DATA_SCRIPT" ] || fail "split_data.py not found in the fetched FAB: $FAB_FILE"

log "Preparing CIFAR10 test data..."
(cd "$SCRIPT_DIR" && python3 "$SPLIT_DATA_SCRIPT")
[ -f "$SCRIPT_DIR/cifar10_train.pt" ] || fail "split_data.py did not create cifar10_train.pt"
[ -f "$SCRIPT_DIR/cifar10_test.pt" ] || fail "split_data.py did not create cifar10_test.pt"
log "Test data prepared."
