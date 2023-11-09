### Migrate Armadillo 2 to Armadillo 3

To export data from and Armadillo 2 server take the following steps:

#### 1. Check if there's enough space left on the server
```
df -h
```
Compare to:
```
du -h /var/lib/minio
```
Available space should be at least twice the size of the MinIO folder. 

#### 2. Backup Armadillo 2 settings
```
mkdir armadillo2-backup 
rsync -avr /usr/share/armadillo armadillo2-backup 
cp /etc/armadillo/application.yml armadillo2-backup/application-armadillo2.yml 
```
N.B.change /usr/share to path matching your local config.

#### 3. Install helper software
Login to your server as root, using ssh. 
```
apt update 
apt install pip 
pip install minio 
pip install fusionauth-client 
pip install simple_term_menu 
```
If you get a purple message asking to update, accept and install everything.
Restart of server is recommended after this.

N.B. Note that the commands in this manual are for Ubuntu, on other linux systems, 
the `apt` command needs to be replaced with another one.

#### 4. Stop all docker images for Armadillo 2
List all docker images
```docker ps -a```

Stop and remove all Armadillo 2 related images (except for MinIO), e.g.
```
docker rm armadillo_auth_1 armadillo_console_1 armadillo_rserver-default_1 armadillo_rserver-mediation_1 armadillo_rserver-exposome_1 armadillo_rserver-omics_1 armadillo_armadillo_1 -f 
```
Check with `docker ps -a` if there are still containers running, if so remove these (except for the MinIO) in the same way as the others.

#### 5. Install armadillo
```
apt update
apt install openjdk-19-jre-headless
apt install docker.io
```
The docker.io step might fail because containerd already exists, if that's the case, remove containerd and try again:
```
apt remove containerd.io
apt install docker.io
```

Get armadillo:
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
    --cleanup \
```
Don't forget to set a proper admin password (use a generator), domain, clientid and clientsecret. The client id and
secret can be found on the lifecycle auth server in the configuration for your server. If you don't have permissions to
receive this, you can ask the support team to get it for you.

Open armadillo in the browser and try to login using basicauth to check if the server is running properly. If it's not
running at all, try:
```
systemctl start armadillo
```

#### 6. Export data from Armadillo 2 into armadillo 3
Look up the user/password in the application.yml of the old armadillo. They're called MinIO access key and  minio
secret key.
```
cat /root/armadillo2-backup/application-armadillo2.yml
```
Do the following step in a separate screen. On ubuntu use:
```
screen
```
Navigate to the armadillo folder:
```
cd /usr/share/armadillo
```
This step will copy Armadillo 2 data from minio into the folder matching of an Armadillo 3 data folder:
```
mkdir data
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/migrate-minio.py  
python3 migrate-minio.py  --minio http://localhost:9000 --target /usr/share/armadillo/data  
```

This might take a couple of minutes. You can detach the screen using `ctrl+a` followed by `d` and reattach it using 
`screen -r`. 

#### 7. Run Armadillo 3 using exported data
Make sure to move the exported data into the new 'data' folder. Optionally you might need to fix user permissions, e.g.:
```
chown armadillo:armadillo -R data 
```
Check if armadillo is running by going to the URL of your server in the browser, login and navigate to the projects tab.

#### 8. Optionally, acquire a permission set from MOLGENIS team
If you previously run central authorisation server with MOLGENIS team, they can provide you with procedure to load 
pre-existing permissions. They will use:
```
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/migrate-auth.py 
python3 migrate-auth.py  --fusion-auth https://lifecycle-auth.molgenis.org --armadillo https://thearmadillourl.net
```
Now check if all users and data are properly migrated. 

#### 9. Cleanup ngnix config

Change `/etc/nginx/sites-available/armadillo.conf` to:
```
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
Note that the `https://` is missing in the server_name part.
NOTE: if port 443 and the SSL certificates are in the old config, you mind have to keep that part, so you shouldn't comment that out. Keep the listen and certificate lines, comment out the rest and paste the config above below the existing config. 

Remove the console, auth and storage file from: `/etc/nginx/sites-enabled/` and `/etc/nginx/sites-available/. 

```
systemctl restart nginx
```

#### 10. Fix application.yml
Make sure the following is added:
```
server:
forward-headers-strategy: framework
```

#### 11. Fix URLs in the lifecycle FusionAuth
Add the following to the config of your server:
`https://yourserver.com/login/oauth2/code/molgenis`

#### 12. Set up profiles
Login to armadillo in the browser. Navigate to the "Profiles" tab. Add a new profile with the following properties:

Name: `xenon`  
Image: `datashield/armadillo-rserver_caravan-xenon:latest`  
Package whitelist: `dsBase`, `resourcer`, `dsMediation`, `dsMTLBase`, `dsSurvival`, `dsExposome`

Assign a random 9-number seed and create and start the container.

#### 13. Remove old MinIO data
First remove the MinIO docker container. First check the name of the container using `docker ps -a`, then:
```
docker rm containername -f
```
After that remove the data:
```rm -Rf /var/lib/minio/ ```
