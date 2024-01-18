# Armadillo installation

> This guide assumes using Ubuntu Server LTS as the installation script expect the `systemd` service. See [Install alternatives](#install-alternatives) below.

## Requirements

## Server resources

You need a server or virtual machine to deploy the Armadillo stack. The specifications of the resource are the following, depending on the participant size of the cohort you are running.

| Participants  | Memory (in GB) | Diskspace (in GB) | CPU cores |
| ------------- | -------------- | ----------------- | --------- |
| 0-20.000      | 8              | 100               | 4         |
| 20.000-70.000 | 16             | 100               | 4         |
| 70.000 >      | 32             | 150               | 8         |

In case of using dsOmics this setup can be rather bigger. Please contact molgenis-operations@umcg.nl for the latest specifications.

## Software requirements

* Java 17 JRE or JDK
* Docker

In addition to these, there are other optional components you may wish to install, such as setting up nginx as a reverse proxy. 

## Domain

An domain or an hostname ie `cohort.armadillo.domain.org` is required to run Armadillo.

This domain is needed for the installation script.

## Authentication

Before we start with the deployment of Armadillo you will need to register your domain that you are going to use with your Armadillo on the DataSHIELD authentication server. This allows you to delegate the authentication and user management. The authorization will still be under the control of the Data Manager (who gets access and who don't get access) within your armadillo installation.

To registrate you will need to send a mail to `molgenis-support@umcg.nl` with the [chosen domains](#domain) and the e-mail adres of the Data Manager who is granted admin permissions in Armadillo. Also add to the mail that you want to register for the the DataSHIELD authentication server and if you belong to a project like Lifecycle, Athlete or Longitools.

When the Armadillo domain is registrerd you will get an mail back with data that need to be inserted in step 2.

The values needed are:

- OIDC service url i.e. https://lifecycle-auth.molgenis.org
- OIDC Client ID
- OIDC Client Secret

## Securing the connection

You need a SSL certificate to configuring the front-end proxy and make the browser use **https** before putting data on the server.

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

#### 2.1 Download the setup script

```
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/armadillo-setup.sh 
```

Or manually download the setup script via right click on this [link](https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/armadillo-setup.sh) (Right mouse 'save as')

Make sure `armadillo-setup.sh` is executable using

```bash
chmod u+x armadillo-setup.sh
```

The installation script requires some arguments:

| Argument         | Application   |
| ---------------- | ------------- |
| admin-user       | Local armadillo admin user            |
| admin-password   | Secure password for the admin user    |
| datadir          | The location where the data is stored. This directory should be have enough diskspace en could be backuped (Default &rarr; /usr/share/armadillo/data)|
| domain           | The URL where armadillo is listening on. For example: cohort.armadillo.domain.org  |
|||
|oidc              | Enable OIDC, see [authentication](#Authentication) |
|oidc_url          | Given oidc URL |
|oidc_clientid     | Given client ID|
|oidc_clientsecret | Given secret ID|

### 2.2 Run the install script

Adapt the following install command to suit your situation. Use `--help` to see all options.

> Note: https://lifecycle-auth.molgenis.org is MOLGENIS provided OIDC service but
you can  also use your own, see FAQ below.

```bash
bash armadillo-setup.sh \
    --admin-user admin \
    --admin-password xxxxx 
    --domain armadillo.cohort.study.com \
    --oidc \
    --oidc_url https://lifecycle-auth.molgenis.org \
    --oidc_clientid clientid \
    --oidc_clientsecret secret \
```

## File locations

The script creates using default values the follow files and directories:

```bash
/etc/armadillo/application.yml
/etc/systemd/system/armadillo.service
/usr/share/armadillo/*
/var/log/armadillo/*
```

## Controlling the Armadillo service

```bash
systemctl status armadillo

systemctl stop armadillo

systemctl start armadillo
```

After the installation is complet armadillo is listening on port 8080. Test the setup by visiting `http://armadillo.cohort.study.com:8080` or type in the terminal `wget http://localhost:8080` to see a text response.

> Note: the Armadillo website is not secure yet so you need to setup a *front-end* proxy.

## Setting up Proxy Server

We highly recommend using `nginx` with MolgenisArmadillo. We use the follow configuration located in `/etc/nginx/sites-available/armadillo.conf`

```nginx
server {
  listen 80;
  server_name armadillo.cohort.study.com
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

> Note: the above is still not secure but not you can reach Armadillo from `http://armadillo.cohort.study.com`.

To secure the communication using https we have a [nginx example](https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/conf/armadillo-nginx.conf)

## Backups

A good start for backuping data is the /usr/share/armadillo and /etc/armadillo. If you gave another datadir as setup option you also should backup this directory. For disaster backups you should contact your IT department.

## Install alternatives

- On local machine using [java](./install/install_java.md)
- Armidillo running as a [Docker](./install/install_docker.md) container.
- [Apache](./install/install_apache.md)

For questions on other linux release you can email molgenis-operations@umcg.nl

## What's next?

* [For the server owner or data manager who need to put data on to the server](https://molgenis.github.io/molgenis-r-armadillo/)
* [For the researcher who want to start analyzing the data on the server](https://molgenis.github.io/molgenis-r-datashield/)