#!/bin/bash

echo "🚀 Armadillo Update started, 👩‍🚀 please fasten you're seatbelts, we're about to take off..."
#### SET OPTS ####
while getopts "p:v:m:u" flag
do
  case "${flag}" in
    p) ARMADILLO_PATH=${OPTARG};;
    v) ARMADILLO_VERSION=${OPTARG};;
    m) MODE=${OPTARG};;
    u) UPDATE=true;;
    a*) echo "❌ ERROR: Invalid argument. Only -p (armadillo path), -v (armadillo version), -m (mode: DEV/PROD) and -u (sets update to true) allowed" & exit_script;;
  esac
done

exit_script() {
  echo "🪂 Exiting..."
      exit 1
}

#### CHECK OPTS ####
if [ -z "$UPDATE" ]; then
  UPDATE=false
fi
if [ -z "$ARMADILLO_PATH" ]; then
    echo "❌ ERROR: Armadillo application path is empty, please specify using [-p]."
    exit_script
fi
if [[ -z "$ARMADILLO_VERSION" && $UPDATE == true ]]; then
    echo "❌ ERROR: Armadillo version to update to is empty, but -u set, please specify using [-v]."
    exit_script
fi
if [ -z "$MODE" ]; then
    echo "‼️ WARN: Mode not set, assuming PROD (production)."
    MODE="PROD"
fi

#### METHODS ####
link_armadillo_version() {
  echo "ℹ️ Setting version to: $ARMADILLO_VERSION"
  echo "🧹 Removing old armadillo"
  rm "$ARMADILLO_PATH/armadillo.jar"
  echo "🔗 Linking new armadillo"
  ln -s -f "$1" "$ARMADILLO_PATH/armadillo.jar"
}

restart_armadillo() {
   if [[ "$MODE" == "PROD" ]]; then
      echo "🛑 Stopping Molgenis Armadillo"
      systemctl stop armadillo
      if [[ $UPDATE == true ]]; then
        link_armadillo_version $1
      fi
      echo "🏁 Starting Molgenis Armadillo 🏎️"
      systemctl start armadillo
    else
      loggedInUser=$( ls -l /dev/console | awk '{print $3}' )
      userID=$( id -u "$loggedInUser" )
      echo "🔃 Working in dev mode, make sure you're running armadillo globally on your mac using launchctl..."
      echo "Killing the armadillo 🔪"
      launchctl bootout "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
      if [[ $UPDATE == true ]]; then
        link_armadillo_version $1
      fi
      echo "Attempting revival 🍃 (violence is never the solution) "
      launchctl bootstrap "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
  fi
}

check_version_exists() {
   # Check if jar available
    VERSION_FOUND=false

    # check if jar is available
    for entry in "$1"/*
    do
      if [[ "$entry" == *$2 ]]; then
        VERSION_FOUND=true
        echo "ℹ️ Armadillo jar for version $3 available: $entry"
      fi
    done
    echo $VERSION_FOUND
}

#### HERE IT STARTS ####
OLD_JAR=""
# Initial timeout: amount of time armadillo can be up in
TIMEOUT=4
if [[ $UPDATE == true ]]; then
  # on PROD location of build and symlink are the same
  BUILD_DIR=$ARMADILLO_PATH
  if [ "$MODE" == "PROD" ]; then
    # linux
    OLD_JAR=$(find "$ARMADILLO_PATH"/armadillo.jar -prune -printf "%l\n")
    else
      # default build path when running with gradle/intellij
      BUILD_DIR="$ARMADILLO_PATH/build/libs"
      # macos
      OLD_JAR=$(stat -f %Y "$ARMADILLO_PATH"/armadillo.jar)
  fi
  # replace v if vx.y.z pattern is used for specifying version
  JAR_NAME="molgenis-armadillo-${ARMADILLO_VERSION/v/}.jar"
  VERSION_FOUND=$( check_version_exists "$BUILD_DIR" "$JAR_NAME" "$ARMADILLO_VERSION" )
  if [[ $VERSION_FOUND ]];
    then
      restart_armadillo "$BUILD_DIR/$JAR_NAME"
    else
      echo "❌ ERROR: No jar available for version $ARMADILLO_VERSION. Please download it."
      exit_script
  fi
  else
    echo "Nothing fancy, just a restart"
    restart_armadillo ""
fi

increase_timeout() {
  TIMEOUT=$(( TIMEOUT * 2))
}

restart_if_down() {
  sleep $TIMEOUT
  # check if server up
  echo "👩‍🔬 Checking if everything went correctly and if Armadillo is up and running 🏃‍➡️..."
  SERVER_UP="$(lsof -i :8080)"
  echo "STATUS: $SERVER_UP"
  # retry every x seconds (going up exponentially until started)
  if [[ ${#SERVER_UP} == 0 ]]; then
    echo "❌ Restart unsuccessful, trying again..."
    if [[ $OLD_JAR != "" ]]; then
      ARMADILLO_VERSION=$(echo "$OLD_JAR" | grep -oE "\d+\.\d+\.\d+")
      echo "🔄 Rolling back to old version: ${ARMADILLO_VERSION}"
    fi
    restart_armadillo "$OLD_JAR"
    increase_timeout
    if [[ $TIMEOUT -gt 3600 ]]; then
      TIME_OF_DEATH=$(date)
      echo "😵 Out of retries. Armadillo should have been revived by now. Time of death: $TIME_OF_DEATH"
      exit_script
    else
      echo "🧪 Checking again in $TIMEOUT seconds... ⏰"
      restart_if_down
    fi
  else
    echo "✅ All done. Thank you for flying with MOLGENIS Airways ✈️"
    exit_script
  fi
}

restart_if_down
