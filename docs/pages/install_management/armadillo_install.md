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


Before deploying **Armadillo**, you need to register the domain that your Armadillo instance will use with an **authentication server**.  
This ensures that users can securely sign in and that authorization decisions (who can access what) remain under the control of the **Data Manager** within your Armadillo installation.

---

### Supported Authentication Providers

Armadillo supports integration with different OpenID Connect (OIDC)–compatible authentication servers, such as:

- [Keycloak](https://www.keycloak.org/)
- [FusionAuth](https://fusionauth.io/)
- Other OIDC-compliant identity providers (e.g. Azure AD, Google Identity, etc.)

---

### Example: Local Keycloak Setup

If you want to run Armadillo locally or test your deployment, you can use the **Keycloak + Armadillo** quick setup provided in our Docker setup.

You’ll find an example in our [documentation](https://molgenis.github.io/molgenis-service-armadillo/pages/install_management/armadillo_docker_install/), which starts both Keycloak and Armadillo and automatically configures OIDC integration.

### Registering with a consortium authentication server (DataSHIELD)

If your organization is part of a consortium that offers a shared authentication server (for example the **DataSHIELD authentication server**), you can register your Armadillo domain with them. This delegates authentication and user management to the central server while your Data Manager continues to control authorization inside Armadillo.

#### What to send when registering

Send an email to the consortium contact (or `support@example.org` if you use our central contact) containing:

- The domain(s) for your Armadillo instance (e.g. `armadillo.example.org`)
- The **email address** of the Data Manager who should receive admin permissions
- A note that you want to register for the **DataSHIELD authentication server**
- (Optional) Which project you belong to (e.g. `LIFECYCLE`, `ATHLETE`, `LONGITOOLS`)

After processing, you will receive the details you need to configure Armadillo, typically:

- **OIDC Service URL** (example: `https://lifecycle-auth.molgenis.org`)
- **OIDC Client ID**
- **OIDC Client Secret**

These values are then inserted into your deployment configuration (see step 2.p in the deployment guide).

---


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
apt install openjdk-21-jre-headless
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

## Metrics 

The Spring Boot Actuator endpoint is by default open to all authenticated users (the health and info endpoint are open to all). To allow for monitoring as well, we added the possibility of configuring an api-token that will give access to the actuator endpoint without logging in otherwise.
To configure this api key, an addition will have to be made to the `application.yml` of Armadillo (just like how the local admin user and password are configured):
```yaml
armadillo:
  api-key: my-secret-api-key
```
After configuring this, the endpoint can be reached:
``` bash
curl 'http://localhost:8080/actuator/prometheus' -i -X GET \ v
 -H 'Accept: text/plain' --header 'X-API-KEY: my-secret-api-key'
```

The endpoint will still be available via basicauth and oauth as well, either logging in and then accessing via the browser or by passing Authorization headers with the correct authorization details.

You can use this in your monitoring application like prometheus.
(Config example for prometheus)
```yaml
scrape_configs:
  - job_name: 'my-armadillo-app'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['armadilo.url-example.com:8080']
```

### Disable or restrict Metrics 

Spring Boot Actuator is accessible for all users in our project (including `/actuator/metrics`).  
Below are ways to **disable** or **restrict** it.

---

#### 1. Disable Metrics 

To remove `/actuator/metrics` from HTTP exposure, add this to `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        exclude: metrics
```

- ✅ Other endpoints (like `/actuator/health`, `/actuator/info`) remain available  
- ❌ `/actuator/metrics` returns `404 Not Found`  

If you want to disable **all metrics collection** (Micrometer), add:

```yaml
management:
  metrics:
    enable:
      all: false
```

---

#### 2. Restrict Metrics: **Nginx as a Reverse Proxy**
At the infrastructure level, you can block or whitelist IPs for Actuator endpoints.

```nginx
location /actuator/metrics {
    allow 192.168.1.0/24;   # whitelist internal network
    deny all;               # block others
    proxy_pass http://localhost:8080/actuator/metrics;
}

location /actuator/ {
    proxy_pass http://localhost:8080/actuator/;
}
```
## Install alternatives

- On local machine using java
- Armadillo running as a [Docker](../install_management/armadillo_docker_install.md) container.
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
