# Armadillo installation

Armadillo requires:

- Java to run the application
- Docker to access the DataSHIELD profiles
- and OIDC for authentication (not needed for local tests).

Below instructions how to run Armadillo directly from Java, as a Docker container, as a service on Ubuntu or from source code.

Note that for production you should add a https proxy for essential security. And you might need to enable 'Docker socket' on your docker service.

We support Ubuntu installations in this document.

## Install Armadillo as service on Ubuntu

We run Armadillo in production as a Linux service on Ubuntu, ensuring it gets restarted when the server is rebooted. You might be able to reproduce also on CentOS (using yum instead of apt).

### 1. Install necessary software

```bash
apt update
apt install openjdk-19-jre-headless
apt install docker.io 
```

Note: you might need 'sudo'

### 2. Run installation script

This step will install most recent [release](https://github.com/molgenis/molgenis-service-armadillo/releases):

#### Download the setup script

```
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/armadillo-setup.sh 
```

#### Run the install script

Adapt the following install command to suit your situation. Use `--help` to see the options.

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
    --cleanup 
```

## Setting up Proxy Server

We highly recommend using `nginx` with MolgenisArmadillo. We have configured it the following way in
` /etc/nginx/sites-available/armadillo.conf`:

```nginx
server {
  listen 80;
  server_name urlofyourserver.org
  include /etc/nginx/global.d/*.conf;
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
