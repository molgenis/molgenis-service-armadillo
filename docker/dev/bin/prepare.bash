#!/usr/bin/env bash

# Store the current working directory
cwd=$(pwd)

# Get the directory of the script
SCRIPT_DIR="$(dirname "$0")"
DOCKER_DIR="$(dirname "$SCRIPT_DIR")"

cd "$DOCKER_DIR" || exit 1

FAKE_DIR="$DOCKER_DIR/fake-tree/"

PROJECT_DIR="$(git rev-parse --show-toplevel)"
echo "Project dir   : $PROJECT_DIR"
echo "Docker        : $DOCKER_DIR"
echo "Script dir    : $SCRIPT_DIR"
echo "Fake tree dir : $FAKE_DIR"

cd "$PROJECT_DIR"
./gradlew docker

rm -rf "${FAKE_DIR:?}/"* || exit 1
# Make docker happy
#cp -r "$PROJECT_DIR/build/docker" "$FAKE_DIR"
# Make sure Armadillo had the system directory
mkdir -p "$FAKE_DIR/data/system"
cp -r "$PROJECT_DIR/data/shared-lifecycle" "$FAKE_DIR/data/"

cd "$cwd" || exit 1

