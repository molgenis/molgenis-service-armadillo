#!/bin/bash

# Update or check only for updates
# We assume all version are 3 numbers x.y.z
#
# Giving an argument that must be a version which forces this version to download.
# - ./armadillo-check-update.sh 3.4.0
# will download version 3.4.0
#
# To test locally
# - set MODE=dev
# - To prevent update ARMADILLO_VERSION_MINIMAL to a future update

ARMADILLO_UPDATER_VERSION=0.1.0

echo "Updater version: $ARMADILLO_UPDATER_VERSION"

# Change mode to dev when testing locally
MODE=prd

DOWNLOAD_ANYWAYS="n"

REQUESTED_VERSION=""
if [ -n "$1" ]
then
  REQUESTED_VERSION="$1"
  echo "Fetch version  : $REQUESTED_VERSION"
  DOWNLOAD_ANYWAYS="y"
fi


# Armadillo variables
ARMADILLO_GITHUB=https://github.com/molgenis/molgenis-service-armadillo

# Minimal version we support
ARMADILLO_VERSION_MINIMAL=3.9999.0

# Change to y to auto upgrade
AUTO_INSTALL=n

# System variables
ARMADILLO_PATH=/usr/share/armadillo
if [ $MODE = "dev" ]; then
  # we assume a git checkout
  ARMADILLO_PATH=../../../build/armadillo-check-update
  mkdir -p "$ARMADILLO_PATH/application"
fi

echo "Mode           : $MODE"
echo "Auto install   : $AUTO_INSTALL"
echo "Armadillo home : $ARMADILLO_PATH"
echo "- " $(cd "$ARMADILLO_PATH/application/" && ls *.jar)
echo "systemd ?      : " $(ls -l /etc/systemd/system/armadillo.service)
echo "cron ?         : " $(ls -l /etc/cron.*/*dillo*)


check_armadillo_update() {
  # Check the running Armadillo version whether it is upgradeable.
  VERSION_USED=$(curl --location --silent --header 'Accept: application/json' http://localhost:8080/actuator/info | sed -e 's/.*"version":"\([^"]*\)".*/\1/')
  echo "Current version: $VERSION_USED"
  echo ""
  GREATEST=$(compare_versions "$VERSION_USED" "$ARMADILLO_VERSION_MINIMAL")
  if [ "$GREATEST" = "$ARMADILLO_VERSION_MINIMAL" ]; then
    echo "Current version is not upgradeable. It must be higher then $ARMADILLO_VERSION_MINIMAL"
    if [ -z "$REQUESTED_VERSION" ] ; then
      exit
    fi
  fi

  # if no pinned version fetch latest.
  NEXT_VERSION="$REQUESTED_VERSION"
  if [ -z "$NEXT_VERSION" ]; then
    # Check the remote latest available version
    LATEST_RELEASE_URL=$ARMADILLO_GITHUB/releases/latest
    echo "Checking for latest release: $LATEST_RELEASE_URL"
    LATEST_RELEASE=$(curl --location --silent --header 'Accept: application/json' $LATEST_RELEASE_URL)

    NEXT_VERSION=$(echo "$LATEST_RELEASE" | sed -e 's/.*"tag_name":"\([^"]*\)".*/\1/' | sed -e s/.*v//)
    echo "Latest release found $NEXT_VERSION"
  fi

  if [ "$REQUESTED_VERSION" != "$NEXT_VERSION" ]; then
    GREATEST=$(compare_versions "$VERSION_USED" "$NEXT_VERSION")
    if [ "$GREATEST" =  "$VERSION_USED" ]; then
      echo "Current version $VERSION_USED is greater then $NEXT_VERSION ... skipping"
      exit
    fi
  fi

  DL_URL="$ARMADILLO_GITHUB/releases/download/v$NEXT_VERSION/molgenis-armadillo-$NEXT_VERSION.jar"

  if validate_url "$DL_URL"; then
  {
    DOWNLOAD_DESTINATION_JAR="$ARMADILLO_PATH/application/armadillo-$NEXT_VERSION.jar"
    if [[ -f "$DOWNLOAD_DESTINATION_JAR" ]]; then
      echo "File already downloaded in $DOWNLOAD_DESTINATION_JAR"
    else
      echo "Downloading $NEXT_VERSION to $DOWNLOAD_DESTINATION_JAR"
      wget -q -O "$DOWNLOAD_DESTINATION_JAR" "$DL_URL"
    fi

    if [ "$AUTO_INSTALL" = "y" ]; then
      # Skip on dev
      [ $MODE = "dev" ] || systemctl stop armadillo

      ln -s -f $DOWNLOAD_DESTINATION_JAR $ARMADILLO_PATH/application/armadillo.jar
      echo "armadillo $NEXT_VERSION updated"

      # Skip on dev
      [ $MODE = "dev" ] || systemctl start armadillo
    fi
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
    # 10# is 10 base number
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
