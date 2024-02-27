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

# Navigate to the git repository directory
cd molgenis-service-armadillo || error "Cannot cd into git repo"

# Assign the input argument to the BRANCH variable
BRANCH="$1"

# Validate and create a valid file name for the branch
# Replace invalid characters with underscores
BRANCH_AS_FILE_NAME=$(echo "$BRANCH" | sed 's/[^a-zA-Z0-9_-]/_/g')

# Checkout the specified branch
git checkout "origin/$BRANCH" || error "No branch named $BRANCH"

# Build the project (excluding tests)
./gradlew clean build -x test || error "Cannot build"

# Copy the JAR file to the specified location
cp build/lib/*.jar "/usr/share/armadillo/application/armadillo_$BRANCH_AS_FILE_NAME.jar" || error "Cannot copy JAR file"

# Create a symbolic link named "armadillo.jar" in the same directory
ln -s "/usr/share/armadillo/application/armadillo_$BRANCH_AS_FILE_NAME.jar" "/usr/share/armadillo/application/armadillo.jar" || error "Cannot create symbolic link"

# Return to the original directory
cd "$original_dir" || error "Cannot switch back to original directory"
