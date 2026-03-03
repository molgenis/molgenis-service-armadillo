#!/usr/bin/env bash
#
# Builds the supernode-app-test image (subprocess mode: app baked into supernode)
# and pushes it to Docker Hub under timmyjc/.
#
# Prerequisites:
#   - Docker running
#   - Logged in to Docker Hub: docker login
#
# Usage:
#   ./scripts/release/flower/build-push-supernode-app.sh [TAG]
#
# Examples:
#   ./scripts/release/flower/build-push-supernode-app.sh          # builds timmyjc/supernode-app-test:0.0.1
#   ./scripts/release/flower/build-push-supernode-app.sh 0.0.2    # builds timmyjc/supernode-app-test:0.0.2
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

APP_DIR="$SCRIPT_DIR/quickstart-pytorch"
DOCKER_HUB_USER="timmyjc"
IMAGE_NAME="supernode-app-test"
TAG="${1:-0.0.1}"

FULL_IMAGE="$DOCKER_HUB_USER/$IMAGE_NAME:$TAG"

log() { echo ">>> $*"; }

# Check if image already exists on Docker Hub
if docker manifest inspect "$FULL_IMAGE" >/dev/null 2>&1; then
  log "Image $FULL_IMAGE already exists on Docker Hub. Skipping build and push."
  exit 0
fi

log "Building supernode-app image: $FULL_IMAGE"
log "Build context: $APP_DIR"

docker build \
  -t "$FULL_IMAGE" \
  -f "$APP_DIR/supernode-app.Dockerfile" \
  "$APP_DIR"

log "Pushing $FULL_IMAGE to Docker Hub..."
docker push "$FULL_IMAGE"

log "Done. Image available at: $FULL_IMAGE"
