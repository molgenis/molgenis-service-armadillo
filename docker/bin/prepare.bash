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
# Make docker happy
#cp -r "$PROJECT_DIR/build/docker" "$FAKE_DIR"
# Make sure Armadillo had the system directory
mkdir -p "$FAKE_DIR/data/system"
cp -r "$PROJECT_DIR/data/shared-lifecycle" "$FAKE_DIR/data/"

if [ "$TARGET_ENV" = "ci" ]; then
  mkdir -p "$FAKE_DIR/cicd/"
  cp "$PROJECT_DIR/scripts/release/release-test.R" "$FAKE_DIR/cicd/"
  cp "$PROJECT_DIR/scripts/release/install_release_script_dependencies.R" "$FAKE_DIR/cicd/"
  cp "$TARGET_DIR/ci.env" "$FAKE_DIR/cicd/.env" || exit 1

  cd "$TARGET_DIR" || exit 1
  docker build . --platform linux/amd64 --tag molgenis/r-cicd || exit 1
fi

cd "$cwd" || exit 1
