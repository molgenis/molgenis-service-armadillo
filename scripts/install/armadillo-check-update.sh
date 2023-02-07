#!/bin/bash

ARMADILLO_UPDATESCR_VERSION=0.0.1

check_self_update() {
    
   
}

check_armadillo_update() {

  FILE=$(readlink "$ARMADILLO_PATH/application/armadillo.jar")
  VERSION_USED=$(curl -L -s -H 'Accept: application/json' -s http://localhost:8080/actuator/info | sed -e 's/.*"version":"\([^"]*\)".*/\1/')
    
  LATEST_RELEASE=$(curl -L -s -H 'Accept: application/json' -s $ARMADILLO_URL/releases/latest)
  ARMADILLO_LATEST_VERSION=$(echo "$LATEST_RELEASE" | sed -e 's/.*"tag_name":"\([^"]*\)".*/\1/' | sed -e s/.*armadillo-service-//)



  echo $ARMADILLO_LATEST_VERSION vs $VERSION_USED
  

  if [[ "$ARMADILLO_LATEST_VERSION" =~ "2.2.3" ]]; then
  {
    
    echo "Armadillo 2 version not supported on this system"
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
      echo "$ARMADILLO_LATEST_VERSION updated"
      systemctl start armadillo

    fi
    
  }
  else
  {
    echo "No armadillo updates found"
  }
  fi

  #echo $VER

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