# Armadillo installation

BEGIN: remove

Documentation bugs:
- ?how to enable docker socket?
- ?apt install nginx versus redhat flavour?
- ?how to enroll OIDC process
- !elaborate in install script!
  - it requires OIDC config otherwise Armadillo crashes
    - removing the section from `/etc/armadillo/application.yml` fixed it
- script installed `datashield/armadillo-rserver` initially
- `./release-test.R` requires xenon profile
  - adding profile `datashield/rock-dolomite-xenon:latest` ends with
    - >   Error: Could not start [xenon]: SyntaxError: The string did not match the expected pattern..
  - expected auto install of rock
- adding profile does not randomize ... it that bad?
END: remove

> This guide assumes using Ubuntu Server LTS. See [Install alternatives](#install-alternatives) below.

Armadillo requires:

- Java to run the application
- Docker to access the DataSHIELD profiles
- and OIDC for authentication (not needed for local tests).
- you might need to enable 'Docker socket' on your docker service.

## Preparation

If you want to use an OIDC authentication service you need the credentials first for setting up Armadillo.

### OIDC

- OIDC service url https://lifecycle-auth.molgenis.org
- OIDC Client ID
- OIDC Client Secret

## Installing Armadillo as service

We run Armadillo in production as a Linux service on Ubuntu, ensuring it gets restarted when the server is rebooted. You might be able to reproduce also on CentOS (using yum instead of apt).

### 1. Install necessary software

```bash
apt update
apt install openjdk-19-jre-headless
apt install docker.io 
```

Note: you might need 'sudo'

### 2. Run installation script

This step will install most recent [release](https://github.com/molgenis/molgenis-service-armadillo/releases)

After installation the Armadillo application is installed with the given configuration and its service.

#### Download the setup script

```
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/armadillo-setup.sh 
```

Make sure `armadillo-setup.sh` is executable using

```bash
chmod u+x armadillo-setup.sh
```

#### Run the install script

Adapt the following install command to suit your situation. Use `--help` to see all options.

> Note: https://lifecycle-auth.molgenis.org is MOLGENIS provided OIDC service but
you can  also use your own, see FAQ below.

```bash
bash armadillo-setup.sh \
    --admin-user admin \
    --admin-password xxxxx 
    --domain my.server.com \
    --oidc \
    --oidc_url https://lifecycle-auth.molgenis.org \
    --oidc_clientid clientid \
    --oidc_clientsecret secret \
```

#### File locations

This creates the follow files/directories

```bash
/etc/armadillo/application.yml
/etc/systemd/system/armadillo.service
/usr/share/armadillo/*
/var/log/armadillo/*
```

## Start and stop armadillo

```bash
systemctl stop armadillo
systemctl start armadillo
```

## Setting up Proxy Server

We highly recommend using `nginx` with MolgenisArmadillo. We have configured it the following way in
` /etc/nginx/sites-available/armadillo.conf`:

```nginx
server {
  listen 80;
  server_name urlofyourserver.org
  include /etc/nginx/conf.d/*.conf;
  location / {
  proxy_pass http://localhost:8080;
  client_max_body_size 0;
  proxy_read_timeout 600s;
  proxy_redirect http://localhost:8080/ $scheme://$host/;
  proxy_set_header Host $host;
  proxy_http_version 1.1;
  }
}
```

## Install alternatives

- On local machine using [java](./install/install_java.md)
- Armidillo running as a [Docker](./install/install_docker.md) container.
- [Apache](./install/install_apache.md)
