#!/bin/bash

# Update or check only for updates
# We assume all version are 3 numbers x.y.z
#
# Giving an argument that must be a version which forces this version to download.
#
# To test locally
# - set MODE=dev
# - To prevent update ARMADILLO_VERSION_MINIMAL to a future update

ARMADILLO_UPDATER_VERSION=0.1.0

echo "Updater version: $ARMADILLO_UPDATER_VERSION"

# Change mode to dev when testing locally
MODE=prd
MODE=dev

PINNED_VERSION=""
if [ -n "$1" ]
then
  PINNED_VERSION="$1"
  echo "Will fetch '$PINNED_VERSION' shortly"
fi



# Armadillo variables
ARMADILLO_GITHUB=https://github.com/molgenis/molgenis-service-armadillo

# Minimal version we support
ARMADILLO_VERSION_MINIMAL=4.0.0

# Change to y to auto upgrade
AUTO_INSTALL=n


# System variables
ARMADILLO_PATH=/usr/share/armadillo
if [ $MODE = "dev" ]; then
  ARMADILLO_PATH=../../../build/armadillo-check-update
  mkdir -p "$ARMADILLO_PATH/application"
fi

check_armadillo_update() {
  # check the local running version
  VERSION_USED=$(curl --location --silent --header 'Accept: application/json' http://localhost:8080/actuator/info | sed -e 's/.*"version":"\([^"]*\)".*/\1/')
  echo "Current running version $VERSION_USED"
  GREATEST=$(compare_versions "$VERSION_USED" "$ARMADILLO_VERSION_MINIMAL")
  if [ "$GREATEST" = "$ARMADILLO_VERSION_MINIMAL" ]; then
    echo "Current version is not upgradeable. It must be higher then $ARMADILLO_VERSION_MINIMAL"
    exit
  fi

  NEXT_VERSION="$PINNED_VERSION"
  if [ -z "$NEXT_VERSION" ]; then
    # Check the remote latest available version
    LATEST_RELEASE_URL=$ARMADILLO_GITHUB/releases/latest
    echo "Checking for latest release: $LATEST_RELEASE_URL"
    LATEST_RELEASE=$(curl --location --silent --header 'Accept: application/json' $LATEST_RELEASE_URL)

    NEXT_VERSION=$(echo "$LATEST_RELEASE" | sed -e 's/.*"tag_name":"\([^"]*\)".*/\1/' | sed -e s/.*v//)
    echo "Latest release found $NEXT_VERSION"
  fi

  if [ "$PINNED_VERSION" != "$NEXT_VERSION" ]; then
    GREATEST=$(compare_versions "$VERSION_USED" "$NEXT_VERSION")
    if [ "$GREATEST" =  "$VERSION_USED" ]; then
      echo "Current version $VERSION_USED is greater then $NEXT_VERSION ... skipping"
      exit
    fi
  fi

  DL_URL="$ARMADILLO_GITHUB/releases/download/v$NEXT_VERSION/molgenis-armadillo-$NEXT_VERSION.jar"

  if validate_url "$DL_URL"; then
  {
    # Skip on dev
    [ $MODE = "dev" ] || systemctl stop armadillo

    DOWNLOAD_DESTINATION_JAR="$ARMADILLO_PATH/application/armadillo-$NEXT_VERSION.jar"
    if [[ -f "$DOWNLOAD_DESTINATION_JAR" ]]; then
      echo "File already downloaded"
    else
      echo "Downloading $DOWNLOAD_DESTINATION_JAR"
      wget -q -O "$DOWNLOAD_DESTINATION_JAR" "$DL_URL"
    fi

    if [ "$AUTO_INSTALL" = "y" ]; then
      ln -s -f $DOWNLOAD_DESTINATION_JAR $ARMADILLO_PATH/application/armadillo.jar
      echo "armadillo $NEXT_VERSION updated"
    fi

    # Skip on dev
    [ $MODE = "dev" ] || systemctl start armadillo
  }
  else
    echo "Somehow $DL_URL is not a valid page."
  fi

}

function validate_url(){
  if [[ `wget -S --spider $1  2>&1 | grep 'HTTP/1.1 200 OK'` ]]; then
    return 0
  else
    return 1
  fi
}

# Return greatest of a.b.c and x.y.z
# Example 1.2.3 < 2.1.0 return 2.1.0
#
# GREATEST=$(compare_versions "3.4.0" "4.0.0")
# echo "Greatest $GREATEST"
compare_versions() {
  # We split the inputs on the dot '.' as an array '()'
  local IFS=.
  # shellcheck disable=SC2206
  local -a ver1=($1)
  # shellcheck disable=SC2206
  local -a ver2=($2)

  for i in {0..2}; do
    if ((10#${ver1[i]} > 10#${ver2[i]})); then
      echo "$1"
      return
    elif ((10#${ver1[i]} < 10#${ver2[i]})); then
      echo "$2"
      return
    fi
  done
  echo "$1"
}


check_armadillo_update
