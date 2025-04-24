# Armadillo installation

!!! note
    This guide assumes you are using Ubuntu Server LTS, the installation script expects the `systemd` service. See [Install alternatives](#install-alternatives) below.

## Requirements

## Server resources

You need a server or virtual machine to deploy the Armadillo stack. The specifications of the resource are the following, depending on the participant size of the cohort you are running.

| Participants  | Memory (in GB) | Diskspace (in GB) | CPU cores |
| ------------- | -------------- | ----------------- | --------- |
| 0-20.000      | 8              | 100               | 4         |
| 20.000-70.000 | 16             | 100               | 4         |
| 70.000 >      | 32             | 150               | 8         |

In case of using dsOmics this setup can be rather bigger. Please [contact](../contact.md) us for the latest specifications.

## Software requirements

* Java 21 JRE or JDK
* Docker

In addition to these, there are other optional components you may wish to install, such as setting up nginx as a reverse proxy.

## Domain

A domain or an hostname ie `cohort.armadillo.domain.org` is required to run Armadillo.

This domain is needed for the installation script.

## Authentication

Before we start with the deployment of Armadillo you will need to register your domain that you are going to use with your Armadillo on the DataSHIELD authentication server. This allows you to delegate the authentication and user management. The authorization will still be under the control of the Data Manager (who gets access and who don't get access) within your armadillo installation.

To register you will need to [contact](../contact.md) us with the [chosen domains](#domain) and the e-mail address of the Data Manager who is granted admin permissions in Armadillo. Also add to the mail that you want to register for the the DataSHIELD authentication server and if you belong to a project like Lifecycle, Athlete or Longitools.

When the Armadillo domain is processed you will get an email back with data that need to be inserted in step 2.

The values needed are:

    - OIDC service url i.e. https://lifecycle-auth.molgenis.org
    - OIDC Client ID
    - OIDC Client Secret

## Securing the connection

You need a SSL certificate to configuring the front-end proxy and make the browser use **https** before putting data on the server.

If SELinux is enabled, run the following command to ensure that the application can connect to required services:

```bash
setsebool -P httpd_can_network_connect on
```

## Installing Armadillo as service

We run Armadillo in production as a Linux service on Ubuntu, ensuring it gets restarted when the server is rebooted. You might be able to reproduce also on CentOS (using `yum` instead of `apt`).

### 1. Install necessary software

```bash
apt update
apt install openjdk-19-jre-headless
apt install docker.io 
```

??? note
    you might need to execute these commands as `sudo`

### 2. Run installation script

This step will install the latest [release](https://github.com/molgenis/molgenis-service-armadillo/releases).

After installation the Armadillo application is installed with the given configuration and its service.

#### 2.1 Download the setup script

```bash
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
|oidc              | Enable OIDC, see [authentication](#authentication) |
|oidc_url          | Given oidc URL |
|oidc_clientid     | Given client ID|
|oidc_clientsecret | Given secret ID|

#### 2.2 Run the install script

Adapt the following install command to suit your situation. Use `--help` to see all options.

??? note
    https://lifecycle-auth.molgenis.org is MOLGENIS provider of the OIDC service but you can also use your own.

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

### File locations

The script creates using default values the follow files and directories:

```bash
ls /etc/armadillo/application.yml
ls /etc/systemd/system/armadillo.service
ls /usr/share/armadillo/*
ls /var/log/armadillo/*
```

## Controlling the Armadillo service

```bash
systemctl status armadillo

systemctl stop armadillo

systemctl start armadillo
```

After the installation is complete Armadillo is listening on port 8080. Test the setup by visiting `http://armadillo.cohort.study.com:8080` or type in the terminal `wget http://localhost:8080` to see a text response.

??? note
    The Armadillo website is not secure yet so you need to setup a *front-end* proxy.

When having setup this *front-end* proxy you could get a bad gateway (see below). This mostly means Armadillo is not ready yet as you already have tested armadillo as mentioned above.

```http
502 Bad Gateway
nginx/1.18.0 (Ubuntu)
```

Recheck with `wget http://localhost:8080` and check the log files.

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

??? note
    The above is still not secure but not you can reach Armadillo from `http://armadillo.cohort.study.com`.

To secure the communication using https we have a [nginx example](https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/conf/armadillo-nginx.conf)

## Backups

A good start for data backup is the `/usr/share/armadillo` and `/etc/armadillo`. If you gave another datadir as setup option you also should backup this directory. For disaster backups you should contact your IT department.

## Install alternatives

- On local machine using java
- Armidillo running as a Docker container.
- [Apache](#apache)

## What's next?

* [For the server owner or data manager who need to put data on to the server](https://molgenis.github.io/molgenis-r-armadillo/)
* [For the researcher who want to start analyzing the data on the server](https://molgenis.github.io/molgenis-r-datashield/)

## Apache

It is possible to run Molgenis Armadillo using Apache, however we do **not** provide support with this configuration.

### Encoding

Apache requires some additional configuration to get the `/storage/projects/{project}/objects/{object}` to work. When this endpoint doesn't work:

    - tables cannot be assigned
    - subsets cannot be created
    - resources cannot be used.

### Tell Armadillo about https

We need to tell Armadillo server how to building URLs.

### Changes to your site-enabled configuration

Your configuration probably should look like this.

```conf
ProxyPreserveHost On

ProxyPass / http://localhost:8080/ nocanon

AllowEncodedSlashes On

RequestHeader set X-Forwarded-Proto https
RequestHeader set X-Forwarded-Port 443
```

After setting this don't forget to restart Apache.