# Armadillo installation
Armadillo requires Java to run, Docker to access the DataSHIELD profiles, and OIDC for authentication (not needed for local tests). Below instructions how to run Armadillo directly from Java, as a Docker container, as a service on Ubuntu or from source code.
Note that for production you should add a https proxy for essential security. And you might need to enable 'Docker socket' on your docker service.

## Run Armadillo using java commandline
Software developers often run Armadillo as java jar file: 

1. Install Java and Docker (for the DataSHIELD profiles)
2. Download Armadillo jar file from [releases](https://github.com/molgenis/molgenis-service-armadillo/releases), for example:
[molgenis-armadillo-3.3.0.jar](https://github.com/molgenis/molgenis-service-armadillo/releases/download/V3.3.0/)
3. Run armadillo using ```java -jar molgenis-armadillo-3.3.0.jar```
4. Go to http://localhost:8080 to see your Armadillo running.

Default Armadillo will start with only 'basic-auth' and user 'admin' with password 'admin'. You can enable 'oidc' for connecting more users. You can change 
by providing and editing [application.yaml](application.template.yml) file
in your working directory and then run command above again.

## Run Armadillo via docker compose
For testing without having to installing Java you can run using docker:

1. Install [docker-compose](https://docs.docker.com/compose/install/)
2. Download this [docker-compose.yml](docker-compose.yml).
3. Execute ```docker-compose up```
4. Once it says 'Started' go to http://localhost:8080 to see your Armadillo running.

The command must run in same directory as downloaded docker file. We made docker available via 'docker.sock' so we can start/stop DataSHIELD profiles. Alternatively you must include the datashield profiles into this docker-compose. You can override all application.yaml settings via environment variables 
(see commented code in docker-compose file).

## Run Armadillo as service on Ubuntu
We run Armadillo in production as a Linux service on Ubuntu, ensuring it gets restarted when the server is rebooted. You might be able to reproduce also on
CentOS (using yum instead of apt).

### 1. Install necessary software
```
apt update
apt install openjdk-19-jre-headless
apt install docker.io 
```
Note: you might need 'sudo'

### 2. Run installation script
This step will install most recent [release](https://github.com/molgenis/molgenis-service-armadillo/releases):
```
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/armadillo-setup.sh 
bash armadillo-setup.sh \
    --admin-user admin \
    --admin-password xxxxx 
    --domain my.server.com \
    --oidc \
    --oidc_url https://lifecycle-auth.molgenis.org \
    --oidc_clientid clientid \
    --oidc_clientsecret secret \
    --cleanup 
```
Note: adapt install command to suit your situation. Use --help to see the options. https://lifecycle-auth.molgenis.org is MOLGENIS provided OIDC service but
you can  also use your own, see FAQ below.
