#!/bin/bash

echo "🚀 Armadillo Update started, 👩‍🚀 please fasten you're seatbelts, we're about to take off..."
# set opts
while getopts "p:v:m:" flag
do
  case "${flag}" in
    p) ARMADILLO_PATH=${OPTARG};;
    v) ARMADILLO_VERSION=${OPTARG};;
    m) MODE=${OPTARG};;
    a*) echo "❌ ERROR: Invalid argument. Only -p (armadillo path), -v (armadillo version) and -m (mode: DEV/PROD) allowed" & exit_script;;
  esac
done

exit_script() {
  echo "🪂 Exiting..."
      exit 1
}

# checks opts
if [ -z "$ARMADILLO_PATH" ]; then
    echo "❌ ERROR: Armadillo application path is empty, please specify using [-p]."
    exit_script
fi

if [ -z "$ARMADILLO_VERSION" ]; then
    echo "❌ ERROR: Armadillo version to update to is empty, please specify using [-v]."
    exit_script
fi

if [ -z "$MODE" ]; then
    echo "‼️ WARN: Mode not set, assuming PROD (production)."
    MODE="PROD"
fi

# set DEV/PROD based variables for where old jar resides and where the jar is located
OLD_JAR=""
# on PROD location of build and symlink are the same
BUILD_DIR=$ARMADILLO_PATH
if [ "$MODE" == "DEV" ]; then
  # default build path when running with gradle/intellij
  BUILD_DIR="$ARMADILLO_PATH/build/libs"
  # macos
  OLD_JAR=$(stat -f %Y armadillo.jar)
else
  # linux
  OLD_JAR=$(find armadillo.jar -prune -printf "%l\n")
fi


echo "Attempting to update MOLGENIS Armadillo to version: $ARMADILLO_VERSION";
echo "Application path: $ARMADILLO_PATH";

VERSION_FOUND=false
# replace v if vx.y.z pattern is used for specifying version
JAR_NAME="molgenis-armadillo-${ARMADILLO_VERSION/v/}.jar"

# check if jar is available
for entry in "$BUILD_DIR"/*
do
  echo "$entry"
  if [[ "$entry" == *$JAR_NAME ]]; then
    VERSION_FOUND=true
    echo "ℹ️ Armadillo jar for version $ARMADILLO_VERSION available: $entry"
  fi
done

link_armadillo_version() {
  echo "ℹ️ Setting version to: $ARMADILLO_VERSION"
  echo "🧹 Removing old armadillo"
  rm "$ARMADILLO_PATH/armadillo.jar"
  echo "🔗 Linking new armadillo"
  ln -s -f "$1" "$ARMADILLO_PATH/armadillo.jar"
}

update_armadillo() {
   if [[ "$MODE" == "PROD" ]]; then
      echo "🛑 Stopping Molgenis Armadillo"
      systemctl stop armadillo
      link_armadillo_version $1
      echo "🏁 Starting Molgenis Armadillo 🏎️"
      systemctl start armadillo
    else
      loggedInUser=$( ls -l /dev/console | awk '{print $3}' )
      userID=$( id -u "$loggedInUser" )
      echo "🔃 Working in dev mode, make sure you're running armadillo globally on your mac using launchctl..."
      echo "Killing the armadillo 🔪"
      launchctl bootout "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
      link_armadillo_version $1
      echo "Attempting revival 🍃 (violence is never the solution) "
      launchctl bootstrap "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
  fi
}

if $VERSION_FOUND;
  then
   update_armadillo "$BUILD_DIR/$JAR_NAME"
  else
    echo "❌ ERROR: No jar available for version $ARMADILLO_VERSION. Please download it."
    exit_script
fi

echo "👩‍🔬 Checking if update went correctly and if Armadillo is up and running 🏃‍➡️..."
SERVER_UP="$(lsof -i :8080)"

if [[ ${#SERVER_UP} == 0 ]]; then
  echo "❌ Update unsuccessful, rolling back to ${OLD_JAR}"
  ARMADILLO_VERSION=$(echo "$OLD_JAR" | grep -oE "\d+\.\d+\.\d+")
  update_armadillo "$OLD_JAR"
  echo "🔄 Rollback done. Now running on version: ${ARMADILLO_VERSION}"
  exit_script
else
  echo "✅ All done. Armadillo 🐉 update successful. Thank you for flying with MOLGENIS Airways ✈️"
  exit_script
fi