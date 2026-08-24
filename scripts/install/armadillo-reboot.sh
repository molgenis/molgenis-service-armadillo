#!/bin/bash

echo "$(date) 🚀 Armadillo Update started, 👩‍🚀 please fasten you're seatbelts, we're about to take off..."
#### SET OPTS ####
while getopts "p:v:m:i:c:u" flag
do
  case "${flag}" in
    p) ARMADILLO_PATH=${OPTARG};;
    v) ARMADILLO_VERSION=${OPTARG};;
    m) MODE=${OPTARG};;
    i) PID=${OPTARG};;
    c) CONFIG_PATH=${OPTARG};;
    u) UPDATE=true;;
    a*) echo "❌ ERROR: Invalid argument. Only -p (armadillo path), -v (armadillo version), -m (mode: DEV/PROD), -i (process id: the id of the armadillo process), -c (path where application.yml resides) and -u (sets update to true) allowed" & exit_script;;
  esac
done

echo "Running with: path: ${ARMADILLO_PATH}, version: ${ARMADILLO_VERSION}, mode: ${MODE}, process id: ${PID}, config path: ${CONFIG_PATH}, update: ${UPDATE}"

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
  echo "🧹 Removing old armadillo: $ARMADILLO_PATH/armadillo.jar"
  rm "$ARMADILLO_PATH/armadillo.jar"
  echo "🔗 Linking new armadillo"
  ln -s -f "$1" "$ARMADILLO_PATH/armadillo.jar"
  LINK_INFO=$(ls -la "$ARMADILLO_PATH/armadillo.jar")
  echo "🖇️ Linked version: $LINK_INFO"
}

restart_armadillo() {
   echo "🛑 Stopping Molgenis Armadillo"
         kill -SIGTERM ${PID}
         if [[ $UPDATE == true ]] && [[ $1 != "" ]]; then
           link_armadillo_version $1
         fi
         echo "🏁🏎️ Molgenis Armadillo will (hopefully) start back up automatically 🤞🏻"
}

check_version_exists() {
   # Check if jar available
    VERSION_FOUND=0
    return $VERSION_FOUND
}

#### HERE IT STARTS ####
OLD_JAR=""
if [[ $UPDATE == true ]]; then
  # on PROD location of build and symlink are the same
  BUILD_DIR=$ARMADILLO_PATH
  if [ "$MODE" == "PROD" ]; then
    # linux
    OLD_JAR=$(find ${ARMADILLO_PATH}/armadillo.jar -prune -printf "%l\n")
    else
      # default build path when running with gradle/intellij
      BUILD_DIR="$ARMADILLO_PATH/build/libs"
      # macos
      OLD_JAR=$(stat -f %Y armadillo.jar)
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