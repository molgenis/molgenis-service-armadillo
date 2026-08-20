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

check_version_exists() {
   # Check if jar available
    VERSION_FOUND=0

    # check if jar is available
    for entry in "$1"/*
    do
      if [[ "$entry" == *$2 ]]; then
        VERSION_FOUND=1
        echo "ℹ️ Armadillo jar for version $3 available: $entry"
      fi
    done
    return $VERSION_FOUND
}

increase_timeout() {
  if [[ TIMEOUT -lt 8000 ]]; then
    TIMEOUT=$(( TIMEOUT * 2))
    else
      echo "❌ ERROR: Timeout exceeds 2 hours, giving up..."
      exit_script
  fi
}

# NOTE: restart_armadillo used to be its own function. It has been inlined at every
# call site below (and inside restart_if_down) so each call site can fall straight
# into restart_if_down instead of returning up a call stack. Each inlined copy drops
# the "if $UPDATE==true && $1 != ''" link check, since at every call site we already
# know statically whether $1 is empty or not.
restart_if_down() {
  sleep $TIMEOUT
  # check if server up
  echo "👩‍🔬 Checking if everything went correctly and if Armadillo is up and running 🏃‍➡️..."
  SERVER_UP="$(lsof -i :8080 | grep java)"
  echo "STATUS: $SERVER_UP"
  # retry every x seconds (going up exponentially until started), only in dev mode, prod will restart differently
  if [[ ${#SERVER_UP} == 0 ]]; then
    echo "❌ Restart unsuccessful, trying again..."
    if [[ $TIMEOUT -gt 30 ]]; then
      echo "🛟 Checking if rollback possible (config or application version)"
      # if attempted update failed, try and roll back old jar
      if [[ $OLD_JAR != "" ]]; then
        ARMADILLO_VERSION=$(echo "$OLD_JAR" | grep -oE "\d+\.\d+\.\d+")
        echo "🩹 Rolling back to old version: ${ARMADILLO_VERSION}"
        # was: restart_armadillo $OLD_JAR
        # $1 ($OLD_JAR) is always non-empty here (we're inside the "$OLD_JAR != """ branch),
        # so the link step is kept unconditionally.
        if [[ "$MODE" == "PROD" ]]; then
          echo "🛑 Stopping Molgenis Armadillo"
          kill -SIGTERM ${PID}
          link_armadillo_version "$OLD_JAR"
          echo "🏁🏎️ Molgenis Armadillo will (hopefully) start back up automatically 🤞🏻"
        else
          loggedInUser=$( ls -l /dev/console | awk '{print $3}' )
          userID=$( id -u "$loggedInUser" )
          echo "🔃 Working in dev mode, make sure you're running armadillo globally on your mac using launchctl..."
          echo "Killing the armadillo 🔪"
          launchctl bootout "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
          link_armadillo_version "$OLD_JAR"
          echo "Attempting revival 🍃 (violence is never the solution) "
          launchctl bootstrap "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
        fi
      # else if application.yml.bak available with date of today, attempt rollback
      elif [[ $CONFIG_PATH != "" ]]; then
        echo "Config path: ${CONFIG_PATH}"
        if [ ! -f "${CONFIG_PATH}"/application.yml.bak ]; then
          echo "❌ Backup config not found!"
        else
          echo "🛂 Checking if config backup was made recently"
          DATE_CONFIG_BACKUP=$(date -r "$CONFIG_PATH"/application.yml.bak "+%m-%d-%Y %H:%M")
          DATE_CONFIG=$(date -r "$CONFIG_PATH"/application.yml "+%m-%d-%Y %H:%M")
          echo "BACKUP MADE: ${DATE_CONFIG_BACKUP}"
          echo "CONFIG MADE: ${DATE_CONFIG}"
          if [[ $DATE_CONFIG_BACKUP == "$DATE_CONFIG" ]]; then
            echo "🩹 Rolling back old config file"
            cp "$CONFIG_PATH/application.yml.bak" "$CONFIG_PATH/application.yml.bak.bak"
            rm "$CONFIG_PATH/application.yml"
            mv "$CONFIG_PATH/application.yml.bak" "$CONFIG_PATH/application.yml"
          fi
        fi
      fi
    fi
    # was: restart_armadillo ""
    # $1 is always empty here, so the link step is dropped entirely.
    if [[ "$MODE" == "PROD" ]]; then
      echo "🛑 Stopping Molgenis Armadillo"
      kill -SIGTERM ${PID}
      echo "🏁🏎️ Molgenis Armadillo will (hopefully) start back up automatically 🤞🏻"
    else
      loggedInUser=$( ls -l /dev/console | awk '{print $3}' )
      userID=$( id -u "$loggedInUser" )
      echo "🔃 Working in dev mode, make sure you're running armadillo globally on your mac using launchctl..."
      echo "Killing the armadillo 🔪"
      launchctl bootout "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
      echo "Attempting revival 🍃 (violence is never the solution) "
      launchctl bootstrap "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
    fi
    increase_timeout
    echo "🧪 Checking again in $TIMEOUT seconds... ⏰"
    restart_if_down
  else
    echo "✅ All done. Thank you for flying with MOLGENIS Airways ✈️"
    exit_script
  fi
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
      # was: restart_armadillo "$BUILD_DIR/$JAR_NAME"
      # $1 is non-empty and we're inside "$UPDATE == true", so the link step is kept unconditionally.
      if [[ "$MODE" == "PROD" ]]; then
        echo "🛑 Stopping Molgenis Armadillo"
        kill -SIGTERM ${PID}
        link_armadillo_version "$BUILD_DIR/$JAR_NAME"
        echo "🏁🏎️ Molgenis Armadillo will (hopefully) start back up automatically 🤞🏻"
        restart_if_down
      else
        loggedInUser=$( ls -l /dev/console | awk '{print $3}' )
        userID=$( id -u "$loggedInUser" )
        echo "🔃 Working in dev mode, make sure you're running armadillo globally on your mac using launchctl..."
        echo "Killing the armadillo 🔪"
        launchctl bootout "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
        link_armadillo_version "$BUILD_DIR/$JAR_NAME"
        echo "Attempting revival 🍃 (violence is never the solution) "
        launchctl bootstrap "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
        restart_if_down
      fi
    else
      echo "❌ ERROR: No jar available for version $ARMADILLO_VERSION. Please download it."
      exit_script
  fi
else
    echo "Nothing fancy, just a restart"
    # was: restart_armadillo ""
    # $1 is always empty here, so the link step is dropped entirely.
    if [[ "$MODE" == "PROD" ]]; then
      echo "🛑 Stopping Molgenis Armadillo"
      kill -SIGTERM ${PID}
      echo "🏁🏎️ Molgenis Armadillo will (hopefully) start back up automatically 🤞🏻"
      restart_if_down
    else
      loggedInUser=$( ls -l /dev/console | awk '{print $3}' )
      userID=$( id -u "$loggedInUser" )
      echo "🔃 Working in dev mode, make sure you're running armadillo globally on your mac using launchctl..."
      echo "Killing the armadillo 🔪"
      launchctl bootout "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
      echo "Attempting revival 🍃 (violence is never the solution) "
      launchctl bootstrap "gui/${userID}" /Library/LaunchAgents/org.molgenis.armadillo.plist
      restart_if_down
    fi
fi