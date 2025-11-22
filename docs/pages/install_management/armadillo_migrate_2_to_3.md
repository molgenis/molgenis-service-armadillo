# Migrate Armadillo 2 to Armadillo 3

Migrating from Armadillo 2 to Armadillo 3 can be done in two variants, a full migration including [projects, users and data](#migrate-projects-users-and-data) or [just projects and their users](#migrate-projects-and-their-users).
Both options require Python (version 3.8) and additional python libraries, described in [Getting started](#getting-started).

## Getting started

To start the migration, python 3.8 is advised together with a number of utilitarian python libraries. Other python
versions might work, but performance has only been tested with python 3.8.

### Install with Python virtual environment

For more info see [python virtual environment](https://docs.python.org/3/library/venv.html).

The following code does not require superuser rights. The code does assume you are already in the [scripts/upgrade](https://github.com/molgenis/molgenis-service-armadillo/tree/master/scripts/upgrade) directory.

```bash
python3 -m venv venv
source ./venv/bin/activate
pip install -r requirements.txt
```

??? tip
    If the installation of one (or more libraries) fails, try to install the libraries one by one.

## Migrate Projects, users and data

### 1. Check if there's enough space left on the server

```bash
df -h
```

Compare to:

```bash
du -h /var/lib/minio
```

??? warning
    Available space should be at least twice the size of the MinIO folder.

### 2. Backup Armadillo 2 settings

```bash
mkdir armadillo2-backup 
rsync -avr /usr/share/armadillo armadillo2-backup 
cp /etc/armadillo/application.yml armadillo2-backup/application-armadillo2.yml 
```

???+ note
    Change `/usr/share` to the path matching your local config.

### 3. Install helper software

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

### 4. Stop all docker images for Armadillo 2

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

### 5. Install armadillo

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

### 6. Export data from Armadillo 2 into armadillo 3

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

This might take a couple of minutes. You can detach the screen using ++ctrl+a++ followed by ++d++ and reattach it using `screen -r`.

### 7. Run Armadillo 3 using exported data

Make sure to move the exported data into the new 'data' folder. Optionally you might need to fix user permissions, e.g.:

```bash
chown armadillo:armadillo -R data 
```

Check if armadillo is running by going to the URL of your server in the browser, login and navigate to the projects tab.

### 8. Optionally, acquire a permission set from MOLGENIS team

If you previously ran a central authorisation server with MOLGENIS team, they can provide you with procedure to load pre-existing permissions. They will use:

```bash
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/upgrade/migrate-auth.py
python3 migrate-auth.py  --fusion-auth https://lifecycle-auth.molgenis.org --armadillo https://thearmadillourl.net
```

Now check if all users and data are properly migrated.

??? note
    If the script fails with a timeout, try pinging the armadillo url and lifecycle auth url to see if they're reachable from the server. In case they are not, you could choose to export the users using the `export-users.py` script locally and then manually enter them into the system.

### 9. Cleanup ngnix config

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

### 10. Fix application.yml

Make sure the following is added:

```bash
server:
forward-headers-strategy: framework
```

### 11. Fix URLs in the lifecycle FusionAuth

Add the following to the config of your server: `https://yourserver.com/login/oauth2/code/molgenis`

### 12. Set up containers

Login to armadillo in the browser. Navigate to the "Profiles" tab. Add a new profile with the following properties:

Name: `xenon`  
Image: `datashield/armadillo-rserver_caravan-xenon:latest`  
Package whitelist: `dsBase`, `resourcer`, `dsMediation`, `dsMTLBase`, `dsSurvival`, `dsExposome`

Assign a random 9-number seed and create and start the container.

### 13. Remove old MinIO data

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

## Migrate Projects and their users

Migration of just the projects and their users (with their corresponding rights) can be done by using [export-users.py](https://github.com/molgenis/molgenis-service-armadillo/blob/master/scripts/upgrade/export-users.py) and [import-users.py](https://github.com/molgenis/molgenis-service-armadillo/blob/master/scripts/upgrade/import-users.py).

???+ warning
    This options does not migrate the data!

### 1. Export Projects and users from Armadillo 2

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

### 2. Import Projects and users TSVs into Armadillo 3

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
