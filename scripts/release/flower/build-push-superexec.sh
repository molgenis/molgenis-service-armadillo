#!/usr/bin/env bash
#
# Builds the superexec-test image from quickstart-pytorch-data and pushes
# to Docker Hub. Copies molgenis-flwr-armadillo source into the build context
# so it can be installed in the container without needing git in the image.
#
# Prerequisites:
#   - Docker running
#   - Logged in to Docker Hub: docker login
#   - molgenis-flwr-armadillo checked out at ../../molgenis-flwr-armadillo
#     (relative to the molgenis-service-armadillo repo root)
#
# Usage:
#   ./scripts/release/flower/build-push-superexec.sh [TAG]
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

APP_DIR="$SCRIPT_DIR/quickstart-pytorch-data"
FLWR_ARMADILLO_DIR="$PROJECT_ROOT/../molgenis-flwr-armadillo"

DOCKER_HUB_USER="timmyjc"
IMAGE_NAME="superexec-data-test"
TAG="${1:-0.0.1}"
FULL_IMAGE="$DOCKER_HUB_USER/$IMAGE_NAME:$TAG"

log() { echo ">>> $*"; }

# Check if image already exists on Docker Hub
if docker manifest inspect "$FULL_IMAGE" >/dev/null 2>&1; then
  log "Image $FULL_IMAGE already exists on Docker Hub. Skipping build and push."
  exit 0
fi

# Copy molgenis-flwr-armadillo source into build context
[ -d "$FLWR_ARMADILLO_DIR" ] || { log "ERROR: molgenis-flwr-armadillo not found at $FLWR_ARMADILLO_DIR"; exit 1; }
log "Copying molgenis-flwr-armadillo into build context..."
rm -rf "$APP_DIR/molgenis_flwr_armadillo"
cp -r "$FLWR_ARMADILLO_DIR" "$APP_DIR/molgenis_flwr_armadillo"

log "Building superexec image: $FULL_IMAGE"
docker build \
  -t "$FULL_IMAGE" \
  -f "$APP_DIR/superexec.Dockerfile" \
  "$APP_DIR"

# Clean up copied source
rm -rf "$APP_DIR/molgenis_flwr_armadillo"

log "Pushing $FULL_IMAGE to Docker Hub..."
docker push "$FULL_IMAGE"

log "Done. Image available at: $FULL_IMAGE"
