#!/bin/bash

ARMADILLO_UPDATESCR_VERSION=0.0.1
ARMADILLO_URL=https://github.com/molgenis/molgenis-service-armadillo/
ARMADILLO_PATH=/usr/share/armadillo




check_armadillo_update() {

  VERSION_USED=$(curl -L -s -H 'Accept: application/json' -s http://localhost:8080/actuator/info | sed -e 's/.*"version":"\([^"]*\)".*/\1/')
  LATEST_RELEASE=$(curl -L -s -H 'Accept: application/json' -s $ARMADILLO_URL/releases/latest)
  ARMADILLO_LATEST_VERSION=$(echo "$LATEST_RELEASE" | sed -e 's/.*"tag_name":"\([^"]*\)".*/\1/' | sed -e s/.*v//)

  if [[ "$ARMADILLO_LATEST_VERSION" =~ 2.2.3 ]]; then
  {
    
    echo "Update of Armadillo 2 version is not supported on this system"
    exit
  }
  fi
  
  if [ "$VERSION_USED" != "$ARMADILLO_LATEST_VERSION" ]; then
  {
       
          
    DL_URL=https://github.com/molgenis/molgenis-service-armadillo/releases/download/v$ARMADILLO_LATEST_VERSION/molgenis-armadillo-$ARMADILLO_LATEST_VERSION.jar
    
    if validate_url "$DL_URL"; then
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



check_armadillo_update