#!/bin/bash

ARMADILLO_UPDATESCR_VERSION=0.0.1

check_self_update() {
  UPDATE_SCRIPT=$(curl -L -s https://raw.githubusercontent.com/DickPostma/molgenis-service-armadillo/installScript/scripts/install/armadillo-check-update.sh)
  UPDATE_VERSION=$(echo "$UPDATE_SCRIPT" | sed -e 's/.*"ARMADILLO_UPDATESCR_VERSION"="\([^"]*\)".*/\1/')

  echo $UPDATE_VERSION
  exit
    
  if validate_url $DL_URL; then
    
    wget -q -O $ARMADILLO_PATH/application/armadillo-update.sh "$DL_URL"
    echo "Update script downloaded"
    echo "1 0 * * 0 bash $ARMADILLO_PATH/application/armadillo-update.sh" >> /etc/cron.d/update-armadillo
  
  else
    echo "[ ERROR ] update script not downloaded"
  fi

   
}

check_armadillo_update() {

  FILE=$(readlink "$ARMADILLO_PATH/application/armadillo.jar")
  VERSION_USED=$(curl -L -s -H 'Accept: application/json' -s http://localhost:8080/actuator/info | sed -e 's/.*"version":"\([^"]*\)".*/\1/')
    
  LATEST_RELEASE=$(curl -L -s -H 'Accept: application/json' -s $ARMADILLO_URL/releases/latest)
  ARMADILLO_LATEST_VERSION=$(echo "$LATEST_RELEASE" | sed -e 's/.*"tag_name":"\([^"]*\)".*/\1/' | sed -e s/.*armadillo-service-//)

    if [[ "$ARMADILLO_LATEST_VERSION" =~ "2.2.3" ]]; then
  {
    
    echo "Update of Armadillo 2 version is not supported on this system"
    exit
  }
  fi
  
  if [ "$VERSION_USED" != "$ARMADILLO_LATEST_VERSION" ]; then
  {
       
        
    DL_URL=https://github.com/molgenis/molgenis-service-armadillo/releases/download/armadillo-service-$ARMADILLO_LATEST_VERSION/armadillo-$ARMADILLO_LATEST_VERSION.jar
          
    
    if validate_url $DL_URL; then
      # Stop armadillo
      systemctl stop armadillo
      wget -q -O $ARMADILLO_PATH/application/armadillo-"$ARMADILLO_LATEST_VERSION".jar "$DL_URL"
      ln -s -f $ARMADILLO_PATH/application/armadillo-"$ARMADILLO_LATEST_VERSION".jar $ARMADILLO_PATH/application/armadillo.jar
      echo "armadillo $ARMADILLO_LATEST_VERSION updated"
      systemctl start armadillo

    fi
    
  }
  else
  {
    echo "No armadillo updates found"
  }
  fi


}

function validate_url(){
  if [[ `wget -S --spider $1  2>&1 | grep 'HTTP/1.1 200 OK'` ]]; then
    return 0
  else
    return 1
  fi
}


check_self_update
check_armadillo_update
