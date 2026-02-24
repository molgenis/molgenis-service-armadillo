# Migrate Armadillo 4 to Armadillo 5

MOLGENIS Armadillo v5.x.y depends on Java 21, support for all older versions of Java are dropped, so if your Armadillo instance is not
running on Java 21 yet, you'll have to update. To do so:

## 1. Get the update script

```bash
cd /root
# Change the versions number v5.x.y
mkdir v5.x.y
cd v5.x.y
```

```bash
# Change the version number v5.x.y then run command
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/v5.x.y/scripts/install/armadillo-check-update.sh
```

Make the script runnable
```bash
chmod u+x armadillo-check-update.sh
```

## 2. Run the update script 

```bash
# Change the version number v5.x.y
./armadillo-check-update.sh 5.x.y
```

Once the script has completed, you can verify that the Armadillo JAR file has been downloaded by checking the directory:

```bash
# See all jar files on your system
ls -ltr /usr/share/armadillo/application/
```

## 3. Make backup of system config
```bash
# Still in the correct directory? (`/root/v5.x.y`)
pwd
```

We make a backup into the same `v5x.y` directory but that is not strictly needed.

```bash
cp -r /usr/share/armadillo/data/system ./
```

```bash
ls system/
# access.json  profiles.json
```

## 4. Stop Armadillo
``` bash
systemctl stop armadillo
```

## 5. Update Java
``` bash
apt update
apt install openjdk-21-jre-headless
```

## 6. Link new version

```bash
# List application files
ls -l /usr/share/armadillo/application/

# Remove the linked file
rm /usr/share/armadillo/application/armadillo.jar

# Attach new linked file and dont forget to change the version number v5.x.y
ln -s /usr/share/armadillo/application/armadillo-5.x.y.jar /usr/share/armadillo/application/armadillo.jar

# Check result
ls -l /usr/share/armadillo/application/
```


## 7 Restart Armadillo

```bash
systemctl start armadillo
systemctl status armadillo
```
