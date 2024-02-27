#!/bin/bash

# Function to display an error message and exit
function error {
  echo "ERROR: $1"
  cd "$original_dir" || exit 1
  exit 1
}

# Check if the correct number of arguments is supplied
if [ $# -ne 1 ]; then
  error "Usage: $0 <branch_name>"
fi

# Store the current directory
original_dir=$(pwd)

# Define paths and filenames
ARMADILLO_DIR="/usr/share/armadillo/application"
BRANCH="$1"
BRANCH_AS_FILE_NAME=$(echo "$BRANCH" | sed 's/[^a-zA-Z0-9_-]/_/g')
JAR_FILE="$ARMADILLO_DIR/armadillo_$BRANCH_AS_FILE_NAME.jar"
SYMLINK_FILE="$ARMADILLO_DIR/armadillo.jar"

# Navigate to the git repository directory
cd molgenis-service-armadillo || error "Cannot cd into git repo"

# Checkout the specified branch
git checkout "origin/$BRANCH" || error "No branch named $BRANCH"

# Build the project (excluding tests)
./gradlew clean build -x test || error "Cannot build"

# Copy the JAR file to the specified location
cp build/libs/*.jar "$JAR_FILE" || error "Cannot copy JAR file"

# Remove the symlink
rm "$SYMLINK_FILE" || error "Cannot remove symbolic link $SYMLINK_FILE"

# Create a symbolic link named "armadillo.jar" in the same directory
ln -s "$JAR_FILE" "$SYMLINK_FILE" || error "Cannot create symbolic link"

# Return to the original directory
cd "$original_dir" || error "Cannot switch back to original directory"

systemctl restart armadillo
