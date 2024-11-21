# Installation, Setup and Management

## Armadillo installation

!!! note
    This guide assumes you are using Ubuntu Server LTS, the installation script expects the `systemd` service. See [Install alternatives](#install-alternatives) below.

### Requirements

### Server resources

You need a server or virtual machine to deploy the Armadillo stack. The specifications of the resource are the following, depending on the participant size of the cohort you are running.

| Participants  | Memory (in GB) | Diskspace (in GB) | CPU cores |
| ------------- | -------------- | ----------------- | --------- |
| 0-20.000      | 8              | 100               | 4         |
| 20.000-70.000 | 16             | 100               | 4         |
| 70.000 >      | 32             | 150               | 8         |

In case of using dsOmics this setup can be rather bigger. Please contact support@molgenis.org for the latest specifications.

### Software requirements

* Java 19 JRE or JDK
* Docker

In addition to these, there are other optional components you may wish to install, such as setting up nginx as a reverse proxy.

### Domain

A domain or an hostname ie `cohort.armadillo.domain.org` is required to run Armadillo.

This domain is needed for the installation script.

### Authentication

Before we start with the deployment of Armadillo you will need to register your domain that you are going to use with your Armadillo on the DataSHIELD authentication server. This allows you to delegate the authentication and user management. The authorization will still be under the control of the Data Manager (who gets access and who don't get access) within your armadillo installation.

To register you will need to send a mail to `support@molgenis.org` with the [chosen domains](#domain) and the e-mail address of the Data Manager who is granted admin permissions in Armadillo. Also add to the mail that you want to register for the the DataSHIELD authentication server and if you belong to a project like Lifecycle, Athlete or Longitools.

When the Armadillo domain is processed you will get an mail back with data that need to be inserted in step 2.

The values needed are:

    - OIDC service url i.e. https://lifecycle-auth.molgenis.org
    - OIDC Client ID
    - OIDC Client Secret

### Securing the connection

You need a SSL certificate to configuring the front-end proxy and make the browser use **https** before putting data on the server.

If SELinux is enabled, run the following command to ensure that the application can connect to required services:

```bash
setsebool -P httpd_can_network_connect on
```

### Installing Armadillo as service

We run Armadillo in production as a Linux service on Ubuntu, ensuring it gets restarted when the server is rebooted. You might be able to reproduce also on CentOS (using `yum` instead of `apt`).

#### 1. Install necessary software

```bash
apt update
apt install openjdk-19-jre-headless
apt install docker.io 
```

??? note
    you might need to execute these commands as `sudo`

#### 2. Run installation script

This step will install the latest [release](https://github.com/molgenis/molgenis-service-armadillo/releases).

After installation the Armadillo application is installed with the given configuration and its service.

##### 2.1 Download the setup script

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
|oidc              | Enable OIDC, see [authentication](#Authentication) |
|oidc_url          | Given oidc URL |
|oidc_clientid     | Given client ID|
|oidc_clientsecret | Given secret ID|

##### 2.2 Run the install script

Adapt the following install command to suit your situation. Use `--help` to see all options.

??? note
    https://lifecycle-auth.molgenis.org is MOLGENIS provided OIDC service but you can  also use your own, see FAQ below.

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

### Controlling the Armadillo service

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

### Setting up Proxy Server

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

### Backups

A good start for data backup is the `/usr/share/armadillo` and `/etc/armadillo`. If you gave another datadir as setup option you also should backup this directory. For disaster backups you should contact your IT department.

### Install alternatives

    - On local machine using [java](./install/install_java.md)
    - Armidillo running as a [Docker](./install/install_docker.md) container.
    - [Apache](./install/install_apache.md)

For questions on other linux release you can email support@molgenis.org

### What's next?

* [For the server owner or data manager who need to put data on to the server](https://molgenis.github.io/molgenis-r-armadillo/)
* [For the researcher who want to start analyzing the data on the server](https://molgenis.github.io/molgenis-r-datashield/)

## Armadillo management

Armadillo has three main screens to manage projects, user access and DataSHIELD profiles:

### Create data access projects

Data managers can use the Armadillo web user interface or [MolgenisArmadillo R client](https://molgenis.github.io/molgenis-r-armadillo) to create 'projects' and upload their data into those. Data tables need to be in parquet format that supports fast selections of the columns (variables) you need for analysis. Other files can be configured as 'resources'.

### Manage user access

Data managers can use the permission screen to give email addresses access to the data. Everybody signs in via single sign on using an OIDC central authentication server such as KeyCloack or Fusion auth that federates to authentication systems of connected institutions, ideally using a federated AAI such as LifeScience AAI.

### Configure DataSHIELD profiles

To analyse data, users must choose a datashield profile. Armadillo owners can use the web user interface to configure new profiles. Assuming you installed docker you can also start/stop these images. Alternatively you can use docker-compose for that. We recommend selecting one of the [DataSHIELD standard profiles](https://www.datashield.org/help/standard-profiles-and-plaforms).

### End users can use Armadillo as any other DataSHIELD server

A researcher connects from an [R client](https://molgenis.github.io/molgenis-r-datashield) to one or multiple Armadillo servers. The data is loaded into an R session on the Armadillo server specifically created for the researcher. Analysis requests are sent to the R session on each Armadillo server. There the analysis is performed and aggregated results are sent back to the client.

## Armadillo minor release update

### Minor Version Upgrade Manual: Procedures for y.z Releases

???+ note
    This manual is intended for minor version updates within the latest major release. For example, you can use it to update from version 4.1 to 4.7.1. For upgrading to a new major version, please refer to the specific manuals dedicated to major version upgrades.

#### Check latest version

For the latest 4.y.z release check [https://github.com/molgenis/molgenis-service-armadillo/releases/latest](https://github.com/molgenis/molgenis-service-armadillo/releases/latest). This will redirect to a v4.y.z page.

### Updating Armadillo

#### 1. Stop docker containers

First, log in to the Armadillo UI and navigate to the Profiles tab. Now, click 'Stop' for each profile listed.

Next, stop any unnecessary containers.

The commands provided are indicative, so adjust them as needed.

```bash
# should return empty list (i.e. default, xenon, rock)
docker container list

# remove containers not needed
docker container stop <id>
```

#### 2. Download required files

Make a note of the version number ie. `v4.7.1` as you need to download some files from the terminal using the update script.

##### 2.1 Update script

You need to be root user.

```bash
cd /root

# Make sure to change the versions number v4.y.z
mkdir v4.y.z
cd v4.y.z

# Check directory location
pwd
```

```bash
# Change the version number v4.y.z then run command
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/v4.y.z/scripts/install/armadillo-check-update.sh
```

Make the script executable:

```bash
chmod u+x armadillo-check-update.sh
```

##### 2.2 Run update script

You can run the following script to download the new Armadillo version.

??? tip
    The output could help us to help you fix problems.

```bash
# Change the version number v4.y.z
./armadillo-check-update.sh 4.y.z
```

Once the script has completed, you can verify that the Armadillo JAR file has been downloaded by checking the directory:

```bash
# See all jar files on your system
ls -ltr /usr/share/armadillo/application/
```

##### 3. Make backup of system config

```bash
# Still in the correct directory? (`/root/v4.y.z`)
pwd
```

We make a backup into the same `v4.y.z` directory but that is not strictly needed.

```bash
cp -r /usr/share/armadillo/data/system ./
```

should result in:

```bash
ls system/
# access.json  profiles.json
```

#### 4. Restart application using new version

Armadillo has not yet been updated, follow the following steps to do so:

##### 4.1 Stop Armadillo

```bash
systemctl stop armadillo
```

##### 4.2 Link new version

```bash
# List application files
ls -l /usr/share/armadillo/application/

# Remove the linked file
rm /usr/share/armadillo/application/armadillo.jar

# Attach new linked file and dont forget to change the version number v4.y.z
ln -s /usr/share/armadillo/application/armadillo-4.y.z.jar /usr/share/armadillo/application/armadillo.jar

# Check result
ls -l /usr/share/armadillo/application/
```

##### 4.3 Restart Armadillo

```bash
systemctl start armadillo
systemctl status armadillo
```

#### 5. Log on to the UI

Go to your armadillo website. Is the version in the left top corner updated? This means the update was successful. We're
almost finished.

#### 6. Start profiles

Login into the website and go to the profiles tab. Now you can start all the profiles again.

Everything should now be working correctly. You can try and login to your server via the central analysis server, using
the `DSMolgenisArmadillo` (2.0.5 or up) package to test.

Enjoy =)
Team Armadillo

## Armadillo migration guides

### Migrate Armadillo 3 to Armadillo 4

Upgrade to Armadillo 4 (rock only)

??? note
    We assume Ubuntu with systemd is used.

The upgrade from Armadillo v3.4 to 4.x is breaking as the profiles must be Rock profiles.

Additionally, when working with an armadillo 4 instance, researchers should update `DSMolgenisArmadillo` to version 2.0.5 (this version is compatible with armadillo 3 as well).

#### Get latest version

For the latest 4.x release check https://github.com/molgenis/molgenis-service-armadillo/releases/latest. This will redirect to a v4.x.y page.

Make a note of the version as you will use this below.

#### 1. Check your profile types

Check if the new profiles are compatible with your needs, these profile names can be edited later on in the manual:

    - `datashield/rock-base:latest`
    - `datashield/rock-dolomite-xenon:latest`

See also DataSHIELD [profiles](https://www.datashield.org/help/standard-profiles-and-plaforms)

#### 2. Check server space

Make sure enough disk space is available for the Rock only images.

##### 2.1 Check disk space

```bash
# Check disk space
df -H
```

If you have 15 GB or more available, you can continue. If you have less available, check `docker image list` to see if you can cleanup some docker images (you only need the latest `datashield/armadillo-rserver` and `datashield/armadillo-rserver_caravan-xenon` for armadillo 3).

##### 2.2 Check docker images

First stop all profiles through the Armadillo UI.

Now that the profiles are not running you can delete the old versions of their docker images.

The command are indicative so change as needed.

```bash
# should return empty list (i.e. default, xenon, rock)
docker container list

# remove containers not needed
docker container stop <id>
docker container rm <id>

# remove unneeded images/profiles (ie. caravan, ...)
docker image list
docker image rm <id>
```

If possible download the new images from shell using `docker pull` beforehand (for minimum downtime):

```bash
docker pull datashield/rock-base:latest
docker pull datashield/rock-dolomite-xenon:latest
```

Check disk space again.

```bash
# Check disk space
df -H
```

#### 3. Download required files

Make a note of the version number ie. `v4.1.3` as you need to download some files from the terminal using the update script.

##### 3.1 Update script

You need to be root user.

```bash
cd /root
# Change the versions number v4.x.y
mkdir v4.x.y
cd v4.x.y

# Check directory location
pwd
```

```bash
# Change the version number v4.x.y then run command
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/v4.x.y/scripts/install/armadillo-check-update.sh
```

Make the script runnable
```bash
chmod u+x armadillo-check-update.sh
```

##### 3.2 Run update script

You can run the following script to download the new Armadillo version.

??? note
    The output could help us to help you fix problems.

```bash
# Change the version number v4.x.y
./armadillo-check-update.sh 4.x.y
```

Once the script has completed, you can verify that the Armadillo JAR file has been downloaded by checking the directory:

```bash
# See all jar files on your system
ls -ltr /usr/share/armadillo/application/
```

#### 4. Config the new version

##### 4.1 application.yml

To compare the latest template to your own configuration, see the troubleshooting section below. The safest way to update armadillo is by fetching the template and filling it in with your configuration using the information in the troubleshooting section. You can try the following first:

Edit the application.yml:

```bash
nano /etc/armadillo/application.yml
```

Below the line `docker-management-enabled: true`, ensure to insert the line `docker-run-in-container: false`. Typically, you'll find these configurations at the beginning of the file.

#### 4.2 Make backup of system config

```bash
# Still in the correct directory? (`/root/v4.x.y`)
pwd
```

We make a backup into the same `v4.x.y` directory but that is not strictly needed.

```bash
cp -r /usr/share/armadillo/data/system ./
```

should result in:

```bash
ls system/
# access.json  profiles.json
```

#### 5. Restart application using new version

Armadillo has not yet been updated, follow the following steps to do so:

##### 5.1 Stop Armadillo

```bash
systemctl stop armadillo
```

##### 5.2 Link new version

```bash
# List application files
ls -l /usr/share/armadillo/application/

# Remove the linked file
rm /usr/share/armadillo/application/armadillo.jar

# Attach new linked file and dont forget to change the version number v4.x.y
ln -s /usr/share/armadillo/application/armadillo-4.x.y.jar /usr/share/armadillo/application/armadillo.jar

# Check result
ls -l /usr/share/armadillo/application/
```

##### 5.3 Restart Armadillo

```bash
systemctl start armadillo
systemctl status armadillo
```

#### 6. Log on to the UI

Go to your armadillo website. Is the version in the left top corner updated? This means the update was successful. We're
almost finished.

#### 7. Update profiles

Login into the website and go to the profiles tab. Here two profiles should be listed: `default` and `xenon`.
Any other profiles can be removed.

1. Edit the default profile.
2. Change the "image" to `datashield/rock-base:latest` and save.
3. Start the default profile.
4. Edit the "xenon" profile.
5. Change the "image" to `datashield/rock-dolomite-xenon:latest` and save.
6. Start the xenon profile.

Everything should now be working correctly. You can try and login to your server via the central analysis server, using
the `DSMolgenisArmadillo` (2.0.5 or up) package to test.

Enjoy =)
Team Armadillo

#### Troubleshooting

##### Logs

Reviewing the log files can provide valuable insights into any issues or activities within the application. If you encounter any errors or unexpected behavior, examining the log files can often help diagnose the problem.

Check log files location

```bash
ls -l /var/log/armadillo/
```

should look something like:

```bash
-rw-r--r-- 1 root      root      111224 Jan 30 11:47 armadillo.log
-rw-r--r-- 1 armadillo armadillo  68872 Jan 30 11:47 audit.log
-rw-r--r-- 1 root      root        8428 Dec 19 11:57 error.log
```

If the `error.log` data/time is around current day/time you have to check this file.

```bash
# See last 100 lines
tail -n 100 /var/log/armadillo/error.log
```

Otherwise, you can look into `armadillo.log`:

```bash
# See last 100 lines
tail -n 100 /var/log/armadillo/armadillo.log
```

or

```bash
# Follow all files for changes (keep open to see activities)
tail -f /var/log/armadillo/*
```

##### Compare application.yml

Although we try to be very complete in this manual, if you run into issues, it might be because a setting was changed
in the application.yml. You can check if application settings has any new entries by first downloading the application template (for reference).

```bash
# Change the version number v4.x.y
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/v4.x.y/application.template.yml
```

To see the difference run:

```bash
diff --side-by-side /etc/armadillo/application.yml application.template.yml
```

Your output should look like output below.

- Left side column is your settings.
- Right side column is our expected settings.
- In the middle some symbols may occur:
  - the `&lt;` means only your settings
  - the `&gt;` means we have a setting (probably added or options)
  - the `|` means both have different values which happens with OICD/oauth settings for sure.

```bash
armadillo:                            armadillo:
  # set this false if you DON'T want Armadillo to create/edit      # set this false if you DON'T want Armadillo to create/edit
  docker-management-enabled: true                  docker-management-enabled: true
....
audit:                                  <
  log:                                  <
    path: /var/log/armadillo/audit.log  <
                                        <
....

storage:                                         storage:
  ## to change location of the data storage        ## to change location of the data storage
  root-dir: /usr/share/armadillo/data           |  root-dir: data
```

Making changes may be a little tricky. You can backup

```bash
cp /etc/armadillo/application.yml ./
# list files
ls .
```

then edit

```bash
nano /etc/armadillo/application.yml
```

### Migrate Armadillo 2 to Armadillo 3

Migrating from Armadillo 2 to Armadillo 3 can be done in two variants, a full migration including [projects, users and data](#migrate-projects-users-and-data) or [just projects and their users](#migrate-projects-and-their-users).
Both options require Python (version 3.8) and additional python libraries, described in [Getting started](#getting-started).

### Getting started

To start the migration, python 3.8 is advised together with a number of utilitarian python libraries. Other python
versions might work, but performance has only been tested with python 3.8.

#### Install with Python virtual environment

For more info see [python virtual environment](https://docs.python.org/3/library/venv.html).

The following code does not require superuser rights. The code does assume you are already in the [scripts/upgrade](https://github.com/molgenis/molgenis-service-armadillo/tree/master/scripts/upgrade) directory.

```bash
python3 -m venv venv
source ./venv/bin/activate
pip install -r requirements.txt
```

??? tip
    If the installation of one (or more libraries) fails, try to install the libraries one by one.

### Migrate Projects, users and data

#### 1. Check if there's enough space left on the server

```bash
df -h
```

Compare to:

```bash
du -h /var/lib/minio
```

??? warning
    Available space should be at least twice the size of the MinIO folder.

#### 2. Backup Armadillo 2 settings

```bash
mkdir armadillo2-backup 
rsync -avr /usr/share/armadillo armadillo2-backup 
cp /etc/armadillo/application.yml armadillo2-backup/application-armadillo2.yml 
```

???+ note
    Change `/usr/share` to the path matching your local config.

#### 3. Install helper software

FIXME: Skip these steps as you have already done installing the python code in a virtual env.

Login to your server as root, using ssh.

```bash
apt update

#apt install pip
#pip install minio
#pip install fusionauth-client
#pip install simple_term_menu
```

If you get a purple message asking to update, accept and install everything.

Restart of server is recommended after this.

??? note
    The commands in this manual are for Ubuntu, on other linux systems, the `apt` command needs to be replaced with the correct one.

#### 4. Stop all docker images for Armadillo 2

List all docker images

```bash
docker ps -a
```

Stop and remove all Armadillo 2 related images (except for MinIO), e.g.

```bash
docker rm armadillo_auth_1 armadillo_console_1 armadillo_rserver-default_1 armadillo_rserver-mediation_1 armadillo_rserver-exposome_1 armadillo_rserver-omics_1 armadillo_armadillo_1 -f 
```

Check with `docker ps -a` if there are still containers running, if so remove these (**except for the MinIO**) in the same way as the others.

???+ warning
    Make sure you do **not** remove the docker instance of MinIO before you migrated the data!

#### 5. Install armadillo

```bash
apt update
apt install openjdk-19-jre-headless
apt install docker.io
```

The docker.io step might fail because containerd already exists, if that's the case, remove containerd and try again:

```bash
apt remove containerd.io
apt install docker.io
```

Get armadillo:

```bash
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/armadillo-setup.sh 
bash armadillo-setup.sh \
    --admin-user admin \
    --admin-password xxxxx 
    --domain my.server.com \
    --oidc \
    --oidc_url https://lifecycle-auth.molgenis.org \
    --oidc_clientid clientid \
    --oidc_clientsecret secret \
    --cleanup \
```

Don't forget to set a proper admin password (use a generator), domain, clientid and clientsecret. The client id and
secret can be found on the lifecycle auth server in the configuration for your server. If you don't have permissions to
receive this, you can ask the support team to get it for you.

Open armadillo in the browser and try to login using basicauth to check if the server is running properly. If it is not
running at all, try:

```bash
systemctl start armadillo
```

#### 6. Export data from Armadillo 2 into armadillo 3

Look up the user/password in the application.yml of the old armadillo. They are called MinIO access key and minio secret key.

```bash
cat /root/armadillo2-backup/application-armadillo2.yml
```

Do the following step in a separate screen. On ubuntu use:

```bash
screen
```

Navigate to the armadillo folder:

```bash
cd /usr/share/armadillo
```

This step will copy Armadillo 2 data from minio into the folder matching of an Armadillo 3 data folder:

```bash
mkdir data
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/upgrade/migrate-minio.py
python3 migrate-minio.py  --minio http://localhost:9000 --target /usr/share/armadillo/data  
```

This might take a couple of minutes. You can detach the screen using ++ctrl+a++ followed by ++d++ and reattach it using 
`screen -r`.

#### 7. Run Armadillo 3 using exported data

Make sure to move the exported data into the new 'data' folder. Optionally you might need to fix user permissions, e.g.:

```bash
chown armadillo:armadillo -R data 
```

Check if armadillo is running by going to the URL of your server in the browser, login and navigate to the projects tab.

#### 8. Optionally, acquire a permission set from MOLGENIS team

If you previously ran a central authorisation server with MOLGENIS team, they can provide you with procedure to load pre-existing permissions. They will use:

```bash
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/upgrade/migrate-auth.py
python3 migrate-auth.py  --fusion-auth https://lifecycle-auth.molgenis.org --armadillo https://thearmadillourl.net
```

Now check if all users and data are properly migrated.

??? note
    If the script fails with a timeout, try pinging the armadillo url and lifecycle auth url to see if they're reachable from the server. In case they are not, you could choose to export the users using the `export-users.py` script locally and then manually enter them into the system.

#### 9. Cleanup ngnix config

Change `/etc/nginx/sites-available/armadillo.conf` to:

```bash
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

??? note
    Note that the `https://` is missing in the server_name part.
??? note
    If port 443 and the SSL certificates are in the old config, you mind have to keep that part, so you should not comment that out. Keep the listen and certificate lines, comment out the rest and paste the config above below the existing config.

Remove the console, auth and storage file from: `/etc/nginx/sites-enabled/` and `/etc/nginx/sites-available/`.

```bash
systemctl restart nginx
```

#### 10. Fix application.yml

Make sure the following is added:

```bash
server:
forward-headers-strategy: framework
```

#### 11. Fix URLs in the lifecycle FusionAuth

Add the following to the config of your server: `https://yourserver.com/login/oauth2/code/molgenis`

#### 12. Set up profiles

Login to armadillo in the browser. Navigate to the "Profiles" tab. Add a new profile with the following properties:

Name: `xenon`  
Image: `datashield/armadillo-rserver_caravan-xenon:latest`  
Package whitelist: `dsBase`, `resourcer`, `dsMediation`, `dsMTLBase`, `dsSurvival`, `dsExposome`

Assign a random 9-number seed and create and start the container.

#### 13. Remove old MinIO data

First remove the MinIO docker container. First check the name of the container using `docker ps -a`, then:

```bash
docker rm containername -f
```

After that remove the data:

```bash
rm -Rf /var/lib/minio/
```

???+ warning
    Be sure you have migrated your data successfully or created a backup prior to deleting your minio data folder!

### Migrate Projects and their users

Migration of just the projects and their users (with their corresponding rights) can be done by using [export-users.py](https://github.com/molgenis/molgenis-service-armadillo/blob/master/scripts/upgrade/export-users.py) and [import-users.py](https://github.com/molgenis/molgenis-service-armadillo/blob/master/scripts/upgrade/import-users.py).

???+ warning
    This options does not migrate the data!

#### 1. Export Projects and users from Armadillo 2

To export users from an Armadillo 2 server, one must use the [export-users.py](https://github.com/molgenis/molgenis-service-armadillo/blob/master/scripts/upgrade/export-users.py) script. `export-users.py` can be used by using the following arguments:

- -f / --fusion-auth **(required)**: The full URL (including http) of the Armadillo 2 server of which you wish to export the Projects and their users from. **Please note that `export-users.py` will prompt to supply the API key for this server once all arguments are valid!**
- -o / --output **(required)**: The output directory in which (unzipped) TSVs will be placed of all projects and their users, with the project name being the TSV name. `export-users.py` will create a new folder in the supplied output folder named: `YYYY-MM-DD`, where `YYYY` is the current year, `MM` is the current month and `DD` is the current day.

**Again, note that `export-users.py` will prompt to supply the API key for the `-f / --fusion-auth` server once all arguments are valid!**

Empty projects (without users) will also be exported as an empty TSV (containing only the header). This is a feature that `import-users.py`, the next step, is able to function with.

Also note that some projects might change in name, as Armadillo 3 is stricter with naming projects.

Example:

```bash
pipenv shell
python3 export-users.py -f https://armadillo2-server.org -o ./armadillo_2_exports
```

#### 2. Import Projects and users TSVs into Armadillo 3

To import users into an Armadillo 3 server, one must use the [import-users.py](https://github.com/molgenis/molgenis-service-armadillo/blob/master/scripts/upgrade/import-users.py) script. `import-users` can be used by using the following arguments:

- -s / --server **(required)**: The full URL (including http) of the Armadillo 3 server of which you wish to import the Projects and their users TSVs in [step 1](#1-export-projects-and-users-from-armadillo-2). **Please note that `import-users.py` will prompt to supply the API key for this server once all arguments are valid!**
- -d / --user-data **(required)**: The directory, including the folder named after the year-month-day combination, where the export TSVs from [step 1](#1-export-projects-and-users-from-armadillo-2) are stored.

**Again, note that `import-users.py` will prompt to supply the API key for the `-s / --server` server once all arguments are valid!**

Empty TSVs from [step 1](#1-export-projects-and-users-from-armadillo-2) will be imported as empty projects with no users.

Example:

```bash
pipenv shell
python3 import-users.py -s https://armadillo3-server.org -d ./armadillo_2_exports/2023-11-09
```


