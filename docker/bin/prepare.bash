#!/usr/bin/env bash

[[ "$1" == "" ]] && exit 1
TARGET_ENV=$1

# Store the current working directory
cwd=$(pwd)

# Get the directory of the script
FULL_PATH="$(realpath "$0")"
SCRIPT_DIR="$(dirname "$FULL_PATH")"
DOCKER_DIR="$(dirname "$SCRIPT_DIR")"
TARGET_DIR="$DOCKER_DIR/$TARGET_ENV"

cd "$TARGET_DIR" || exit 1

FAKE_DIR="$TARGET_DIR/fake-tree/"

PROJECT_DIR="$(git rev-parse --show-toplevel)"
echo "Project dir   : $PROJECT_DIR"
echo "Script dir    : $SCRIPT_DIR"
echo "Target        : $TARGET_DIR"
echo "Fake tree dir : $FAKE_DIR"

cd "$PROJECT_DIR"
./gradlew clean build docker

rm -rf "${FAKE_DIR:?}/"* || exit 1

# Make sure Armadillo has needed directories and files
ARMADILLO="$FAKE_DIR/armadillo/"
# System dir must exists
mkdir -p "$ARMADILLO/data/system" || exit 1
# lifecycle data must exits
cp -r "$PROJECT_DIR/data/shared-lifecycle" "$ARMADILLO/data/" || exit 1
mkdir -p "$ARMADILLO/logs" || exit 1
mkdir -p "$ARMADILLO/config" || exit 1
# Specific application.yml file is needed
cp -r "$TARGET_DIR/application.yml" "$ARMADILLO/config" || exit 1

set -x
if [ "$TARGET_ENV" = "ci" ]; then
  CICD="$FAKE_DIR/cicd"

  # expected by `release-test.R`
  mkdir -p "$CICD/armadillo" || exit 1
  cp -r "$ARMADILLO/data" "$CICD/" || exit 1

  BIN_DIR="$CICD/scripts/release/"
  mkdir -p "$BIN_DIR" || exit 1

  LOG_DIR="$CICD/log/"
  mkdir -p "$LOG_DIR" || exit 1

  cp "$PROJECT_DIR/scripts/release/release-test.R" "$BIN_DIR/" || exit 1
  cp "$PROJECT_DIR/scripts/release/install_release_script_dependencies.R" "$BIN_DIR/" || exit 1
  cp "$TARGET_DIR/armadillo-ready.bash" "$BIN_DIR/" || exit 1
  cp "$TARGET_DIR/ci.env" "$BIN_DIR/.env" || exit 1

  cd "$TARGET_DIR" || exit 1
  docker build . --platform linux/amd64 --tag molgenis/r-cicd || exit 1
fi

cd "$cwd" || exit 1
