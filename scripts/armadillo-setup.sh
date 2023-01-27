#!/bin/bash

ARMADILLO_SETUP_VER=1.0.1
ARMADILLO_URL=https://github.com/molgenis/molgenis-service-armadillo/
ARMADILLO_PROFILE=default
ARMADILLO_PATH=/usr/share/armadillo
ARMADILLO_CFG_PATH=/etc/armadillo
ARMADILLO_SYS_USER=armadillo
ARMADILLO_LOG_PATH=/var/log/armadillo
ARMADILLO_AUDITLOG=$ARMADILLO_LOG_PATH/audit.log
ARMADILLO_DATADIR=$ARMADILLO_PATH/data


handle_args() {
    while :
    do
        case "$1" in 
        --version)
          ARMADILLO_VERSION=$2
          shift 2
          ;;
        --admin-user)
          ARMADILLO_ADMIN=$2
          shift 2
          ;;
        --admin-password)
          ARMADILLO_ADMIN_PW=$2
          shift 2
          ;;
        --domain)
          ARMADILLO_DOMAIN=$2
          shift 2
          ;;
        --datadir)
          ARMADILLO_DATADIR=$2
          shift 2
          ;;
        --oidc)
          ARMADILLO_OIDC_ENABLED=1
          shift
          ;;
        --oidc_url)
          OIDC_ISSUER_URL=$2
          shift 2
          ;;
        --oidc_clientid)
          OIDC_CLIENTID=$2
          shift 2
          ;;
        --oidc_clientsecret)
          OIDC_CLIENTSECRET=$2
          shift 2
          ;;
        --cleanup)
          ARMADILLO_CLEANUP=1
          shift          ;;

        -h | --help)
          parameters_help
          exit 0
          ;;
        -*)
          parameters_help
          exit 0
          ;;
        *)
        break;
        esac
        
    done
    if [ ! "$ARMADILLO_CLEANUP" ]; then
      if [ ! "$ARMADILLO_DOMAIN" ] || [ ! "$ARMADILLO_ADMIN_PW" ]; then
        echo "Arguments --domain --admin-password must be provided"
        echo "You need a host or domain to use Armadillo. Example: cohort.armadillo.organisation.com"
        echo "Also for security reasons you must provide a secure admin password"
        parameters_help; 
        exit 1;
      fi
    fi
    if [ "$ARMADILLO_OIDC_ENABLED" ]; then
      if [ ! "$OIDC_CLIENTID" ] || [ ! "$OIDC_ISSUER_URL" ] || [ ! "$OIDC_CLIENTSECRET" ]; then
        echo "OIDC Option called but mandatory config items are missing --oidc_url <issuer_url> --oidc_clientid <client_id> --oidc_clientsecret <secret> "
        exit 1;
      fi
    fi


}


setup_environment() {
    mkdir -p $ARMADILLO_PATH/application
    mkdir -p $ARMADILLO_PATH/services
    mkdir -p "$ARMADILLO_LOG_PATH"
    mkdir -p "$ARMADILLO_CFG_PATH"
    mkdir -p "$ARMADILLO_DATADIR"
    useradd -rs /bin/false "$ARMADILLO_SYS_USER"
    chgrp -R "$ARMADILLO_SYS_USER" "$ARMADILLO_PATH"
    chgrp -R "$ARMADILLO_SYS_USER" "$ARMADILLO_CFG_PATH"
    chgrp -R "$ARMADILLO_SYS_USER" "$ARMADILLO_LOG_PATH"
    chgrp -R "$ARMADILLO_SYS_USER" "$ARMADILLO_DATADIR"

    chmod g+rw "$ARMADILLO_LOG_PATH"
    chmod g+rw "$ARMADILLO_DATADIR"
    usermod -aG docker "$ARMADILLO_SYS_USER"
    echo "Environment is being set up correctly"     
}

setup_systemd() {
    # Set the systemd version. In future download from github? Example:
    #curl -fsSL -o /etc/systemd/system/armadillo.service https://github.com/molgenis-service-armadillo/raw/branch/master/dist/etc/systemd/system/armadillo.service
    cat  > /etc/systemd/system/armadillo.service << EOF
[Unit]
Description=DataSHIELD Armadillo 3
After=syslog.target

[Service]
User=$ARMADILLO_SYS_USER
Environment=SPRING_PROFILES_ACTIVE=$ARMADILLO_PROFILE
Environment=SPRING_CONFIG_LOCATION=$ARMADILLO_CFG_PATH/application.yml
WorkingDirectory=$ARMADILLO_PATH
ExecStart=java -jar $ARMADILLO_PATH/application/armadillo.jar
StandardOutput=append:$ARMADILLO_LOG_PATH/armadillo.log
StandardError=append:$ARMADILLO_LOG_PATH/error.log
Type=simple
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target

EOF

systemctl daemon-reload
echo "Armadillo Installed under systemd"
}

setup_armadillo_config() {
  if [ "$ARMADILLO_OIDC_ENABLED" ]; then
    wget -q -O $ARMADILLO_CFG_PATH/application.yml https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/scripts/conf/application-oidc.yml
    sed -i -e 's|@ISSUERURL@|'"$OIDC_ISSUER_URL"'|g' $ARMADILLO_CFG_PATH/application.yml
    sed -i -e 's/@CLIENTID@/'"$OIDC_CLIENTID"'/' $ARMADILLO_CFG_PATH/application.yml
    sed -i -e 's/@CLIENTSECRET@/'"$OIDC_CLIENTSECRET"'/' $ARMADILLO_CFG_PATH/application.yml
    sed -i -e 's/@ARMADILLODOMAIN@/'"$ARMADILLO_DOMAIN"'/' $ARMADILLO_CFG_PATH/application.yml
    
  else
    wget -q -O /etc/armadillo/application.yml https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/scripts/conf/application.yml
  fi
  
  SEED=$(tr -cd '[:digit:]' < /dev/urandom | fold -w 9 | head -n 1)
   if [ ! "$ADMINUSER" ]; then 
    ADMINUSER="admin"
  else
    ADMINUSER=$ARMADILLO_ADMIN
  fi

  sed -i -e 's|@LOGPATH@|'"$ARMADILLO_LOG_PATH"'|' $ARMADILLO_CFG_PATH/application.yml
  sed -i -e 's/@ADMINUSER@/'"$ADMINUSER"'/' $ARMADILLO_CFG_PATH/application.yml
  sed -i -e 's/@ADMINPASS@/'"${ARMADILLO_ADMIN_PW}"'/' $ARMADILLO_CFG_PATH/application.yml
  sed -i -e 's|@DATADIR@|'"$ARMADILLO_DATADIR"'|' $ARMADILLO_CFG_PATH/application.yml
  sed -i -e 's/@SEED@/'"$SEED"'/' $ARMADILLO_CFG_PATH/application.yml
  sed -i -e 's|@AUDITLOG@|'"$ARMADILLO_AUDITLOG"'|' $ARMADILLO_CFG_PATH/application.yml
  
  echo "Config downloaded"

}
setup_nginx_config() {
  wget -q -O /etc/nginx/sites-enabled/armadillo.conf https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/scripts/conf/armadillo-nginx.conf
  
  sed -i -e 's/@ARMADILLO_URL@/'"$ARMADILLO_DOMAIN"'/' /etc/nginx/sites-enabled/armadillo.conf
  systemctl reload nginx
  echo "Config downloaded"

}

download_armadillo() {
  
  if [ -z "$ARMADILLO_VERSION" ]; then
    LATEST_RELEASE=$(curl -L -s -H 'Accept: application/json' -s $ARMADILLO_URL/releases/latest)
    #ARMADILLO_VERSION=$(echo "$LATEST_RELEASE" | sed -e 's/.*"tag_name":"\([^"]*\)".*/\1/')
    ARMADILLO_VERSION=$(echo "$LATEST_RELEASE" | sed -e 's/.*"tag_name":"\([^"]*\)".*/\1/')
    
    if [[ "$ARMADILLO_VERSION" =~ 'armadillo-service-2' ]]; then
      echo "Armadillo version 2 not supported! Please use provide an armadillo 3 version with --version"
      exit 1;
    fi
    DL_URL=https://github.com/molgenis/molgenis-service-armadillo/releases/download/armadillo-service-$ARMADILLO_VERSION/armadillo-$ARMADILLO_VERSION.jar
  else
    DL_URL=https://github.com/molgenis/molgenis-service-armadillo/releases/download/armadillo-service-$ARMADILLO_VERSION/armadillo-$ARMADILLO_VERSION.jar
    
  fi 
  wget -q -O $ARMADILLO_PATH/application/armadillo-"$ARMADILLO_VERSION".jar "$DL_URL"
  ln -s $ARMADILLO_PATH/application/armadillo-"$ARMADILLO_VERSION".jar $ARMADILLO_PATH/application/armadillo.jar
  echo "$ARMADILLO_VERSION downloaded"
}


check_req() {
  for COMMAND in "nginx" "java" "wget" "docker" "curl" "whoami"; do
    command_exists "${COMMAND}"
  done

  if [ "$(whoami)" != 'root' ]; then
    echo 'Please run this script with root or sudo rights!'
    exit 1;
  fi

}



command_exists() {
    # check if command exists and fail otherwise
    command -v "$1" >/dev/null 2>&1
    if [ $? -ne 0 ]; then
        echo "I require $1 but it's not installed. Abort."
        exit 1
    fi
}


cleanup(){
  if [ "$ARMADILLO_CLEANUP" ]; then
    echo "--cleanup variable defined! Are you sure. Potential data loss? Type y/N"
    read -r -p "Type y/N" response
      if [ "$response" = "y" ]
        then
            systemctl stop armadillo
            systemctl disable armadillo
            rm -Rf $ARMADILLO_PATH
            rm -Rf $ARMADILLO_LOG_PATH
            userdel $ARMADILLO_SYS_USER
            rm -Rf /etc/systemd/system/armadillo.service
            systemctl daemon-reload
            rm -Rf $ARMADILLO_LOG_PATH
            rm /etc/nginx/sites-enabled/armadillo.conf
            systemctl reload nginx
            echo "Armadillo cleaned!"
        else
          echo "No cleanup .. please remove the --cleanup argument"
          exit 0
      fi
  fi
}

startup_armadillo() {
  systemctl enable armadillo
  systemctl start armadillo
  echo "Armadillo started"

}

#Parameters passed in help
parameters_help() {

    echo 'Usage: ./armadillo.sh PARAMS 
       example ./armadillo --admin-user admin --admin-password welcome01 --domain armadillo.cohort.study.com'
    echo
    echo 'Install Script for Armadillo Service'
    echo
    echo 'Params:'
    echo
    echo '    --version armadillo_version   Specify witch version to install'
    echo '    --admin-user user             Specify the Basic-Auth admin user'
    echo '    --admin-password pass         password for the admin user'
    echo '    --datadir /storage/dir        If defined this would be the Location to store the data otherwise it would be /usr/share/armadillo/data'
    echo '    --domain                      URL domain witch is used for accessing armadillo'
    echo ''
    echo '    --oidc                          For central authentication you can enable oidc'
    echo '      --oidc_url                    URL where the oidc server is listening on'
    echo '      --oidc_clientid               client id of the oidc config'
    echo '      --oidc_clientsecret           secret of the client'
    

    
}




if [ "$#" -eq 0 ]; then
    echo 'No parameters provided, please provide the correct parameters'
    parameters_help
    exit 0
fi


handle_args "$@"
check_req
cleanup
setup_environment
download_armadillo
setup_update
setup_armadillo_config
setup_nginx_config
setup_systemd
startup_armadillo

