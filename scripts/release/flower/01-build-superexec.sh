#!/usr/bin/env bash
#
# Build the superexec Docker image and push to Docker Hub.
#
# Copies molgenis-flwr-armadillo source into the build context, builds the
# image, pushes it, then cleans up.
#
# Prerequisites:
#   - Docker running and logged in (docker login)
#   - molgenis-flwr-armadillo checked out at ../../molgenis-flwr-armadillo
#     (relative to the molgenis-service-armadillo repo root)
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

# --- Preflight ---------------------------------------------------------------

docker info >/dev/null 2>&1 || fail "Docker is not running."
[ -d "$FLWR_ARMADILLO_DIR" ] || fail "molgenis-flwr-armadillo not found at $FLWR_ARMADILLO_DIR"
[ -f "$FLWR_APP_DIR/superexec.Dockerfile" ] || fail "superexec.Dockerfile not found in $FLWR_APP_DIR"

# --- Copy molgenis-flwr-armadillo into build context -------------------------

log "Copying molgenis-flwr-armadillo into build context..."
rm -rf "$FLWR_APP_DIR/molgenis_flwr_armadillo"
rsync -a --exclude 'venv/' --exclude '.venv/' --exclude '__pycache__/' --exclude '*.pyc' --exclude '.git/' \
  "$FLWR_ARMADILLO_DIR/" "$FLWR_APP_DIR/molgenis_flwr_armadillo/"

# --- Build -------------------------------------------------------------------

log "Building superexec image: $SUPEREXEC_IMAGE"
docker build \
  -t "$SUPEREXEC_IMAGE" \
  -f "$FLWR_APP_DIR/superexec.Dockerfile" \
  "$FLWR_APP_DIR"

# --- Clean up build context --------------------------------------------------

rm -rf "$FLWR_APP_DIR/molgenis_flwr_armadillo"

# --- Push to Docker Hub ------------------------------------------------------

log "Pushing $SUPEREXEC_IMAGE to Docker Hub..."
docker push "$SUPEREXEC_IMAGE"

log "Done. Image pushed: $SUPEREXEC_IMAGE"
