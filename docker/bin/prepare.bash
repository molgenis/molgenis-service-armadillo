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
./gradlew docker

rm -rf "${FAKE_DIR:?}/"* || exit 1

# Make sure Armadillo has the system directory
ARMADILLO="$FAKE_DIR/armadillo/"
mkdir -p "$ARMADILLO/data/system" || exit 1
cp -r "$PROJECT_DIR/data/shared-lifecycle" "$ARMADILLO/data/" || exit 1
mkdir -p "$ARMADILLO/logs" || exit 1
mkdir -p "$ARMADILLO/config" || exit 1

set -x
if [ "$TARGET_ENV" = "ci" ]; then

  # expected by `release-test.R`
  mkdir -p "$FAKE_DIR/cicd/armadillo" || exit 1
  cp -r "$ARMADILLO/data" "$FAKE_DIR/cicd/" || exit 1

  BIN_DIR="$FAKE_DIR/cicd/scripts/release/"
  mkdir -p "$BIN_DIR" || exit 1

  LOG_DIR="$FAKE_DIR/cicd/log/"
  mkdir -p "$LOG_DIR" || exit 1

  cp "$PROJECT_DIR/scripts/release/release-test.R" "$BIN_DIR/" || exit 1
  cp "$PROJECT_DIR/scripts/release/install_release_script_dependencies.R" "$BIN_DIR/" || exit 1
  cp "$TARGET_DIR/armadillo-ready.bash" "$BIN_DIR/" || exit 1
  cp "$TARGET_DIR/ci.env" "$BIN_DIR/.env" || exit 1

  cd "$TARGET_DIR" || exit 1
  docker build . --platform linux/amd64 --tag molgenis/r-cicd || exit 1
fi

cd "$cwd" || exit 1
