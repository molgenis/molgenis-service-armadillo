#!/usr/bin/env bash

function fail {
  echo "Failed $1"
  exit 1
}

CICD_VERSION="latest"
CICD_IMAGE="molgenis/r-cicd"

TARGET_ENV="ci"

# Store the current working directory
cwd=$(pwd)

# Get the directory of the script
PROJECT_DIR="$(git rev-parse --show-toplevel)"
BUILD_ROOT="$PROJECT_DIR/build/docker"
ARMADILLO_COMPOSE_BUILD_DIR="$BUILD_ROOT/armadillo-compose"
DOCKER_DIR="$PROJECT_DIR/docker"
TARGET_DIR="$DOCKER_DIR/$TARGET_ENV"

cd "$TARGET_DIR" || fail "Cannot jump into $TARGET_DIR"

mkdir -p "$ARMADILLO_COMPOSE_BUILD_DIR" || exit 1

echo "Project dir           : $PROJECT_DIR"
echo "Armadillo compose dir : $ARMADILLO_COMPOSE_BUILD_DIR"
echo "Target                : $TARGET_DIR"

cd "$PROJECT_DIR" || fail "$PROJECT_DIR does not exists"

rm -rf "${ARMADILLO_COMPOSE_BUILD_DIR:?}/"* || exit 1

# Make sure Armadillo has needed directories and files
ARMADILLO="$ARMADILLO_COMPOSE_BUILD_DIR/armadillo/"
# System dir must exists
mkdir -p "$ARMADILLO/data/system" || exit 1
# lifecycle data must exits
cp -r "$PROJECT_DIR/data/shared-lifecycle" "$ARMADILLO/data/" || exit 1
mkdir -p "$ARMADILLO/logs" || exit 1
mkdir -p "$ARMADILLO/config" || exit 1
# Specific application.yml file is needed
cp -r "$TARGET_DIR/application.yml" "$ARMADILLO/config" || exit 1

cp -r "$TARGET_DIR/docker-compose.yml" "$ARMADILLO_COMPOSE_BUILD_DIR" || exit 1
cp "$TARGET_DIR/armadillo-compose.md" "$ARMADILLO_COMPOSE_BUILD_DIR/README.md" || exit 1
cp "$BUILD_ROOT"/*.jar "$ARMADILLO_COMPOSE_BUILD_DIR/" || exit 1
cp "$BUILD_ROOT/Dockerfile" "$ARMADILLO_COMPOSE_BUILD_DIR/" || exit 1

CICD_DIR="$BUILD_ROOT/cicd"
rm -rf "${CICD_DIR:?}/"* || exit 1

# expected by `release-test.R`
ARMADILLO_ROOT="$CICD_DIR/armadillo"
mkdir -p "$ARMADILLO_ROOT" || exit 1
cp -r "$ARMADILLO/data" "$ARMADILLO_ROOT/" || exit 1

BIN_DIR="$ARMADILLO_ROOT/scripts/release/"
mkdir -p "$BIN_DIR" || exit 1

LOG_DIR="$ARMADILLO_ROOT/log/"
mkdir -p "$LOG_DIR" || exit 1

cp "$PROJECT_DIR/scripts/release/release-test.R" "$BIN_DIR/" || exit 1
cp -r "$PROJECT_DIR/scripts/release/test-cases" "$BIN_DIR/" || exit 1
cp "$PROJECT_DIR/scripts/release/install_release_script_dependencies.R" "$BIN_DIR/" || exit 1
cp "$TARGET_DIR/armadillo-ready.bash" "$BIN_DIR/" || exit 1
cp "$TARGET_DIR/ci.env" "$BIN_DIR/.env" || exit 1

cp "$TARGET_DIR/Dockerfile" "$CICD_DIR/"
cd "$CICD_DIR" || exit 1
docker build . --platform linux/amd64 --tag "$CICD_IMAGE:$CICD_VERSION" || fail "Unable to build R CICD image"

cd "$cwd" || exit 1
