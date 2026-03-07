# Armadillo minor release update

## Minor Version Upgrade Manual: Procedures for y.z Releases

???+ note
    This manual is intended for minor version updates within the latest major release. For example, you can use it to update from version 4.1 to 4.7.1. For upgrading to a new major version, please refer to the specific manuals dedicated to major version upgrades.

### Check latest version

For the latest 4.y.z release check [https://github.com/molgenis/molgenis-service-armadillo/releases/latest](https://github.com/molgenis/molgenis-service-armadillo/releases/latest). This will redirect to a v4.y.z page.

## Updating Armadillo

### 1. Stop docker containers

First, log in to the Armadillo UI and navigate to the Profiles tab. Now, click 'Stop' for each profile listed.

Next, stop any unnecessary containers.

The commands provided are indicative, so adjust them as needed.

```bash
# should return empty list (i.e. default, xenon, rock)
docker container list

# remove containers not needed
docker container stop <id>
```

### 2. Download required files

Make a note of the version number ie. `v4.7.1` as you need to download some files from the terminal using the update script.

#### 2.1 Update script

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

#### 2.2 Run update script

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

#### 3. Make backup of system config

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
# access.json  containers.json
```

### 4. Restart application using new version

Armadillo has not yet been updated, follow the following steps to do so:

#### 4.1 Stop Armadillo

```bash
systemctl stop armadillo
```

#### 4.2 Link new version

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

#### 4.3 Restart Armadillo

```bash
systemctl start armadillo
systemctl status armadillo
```

### 5. Log on to the UI

Go to your armadillo website. Is the version in the left top corner updated? This means the update was successful.

### 6. Start containers

Login into the website and go to the containers tab. Now you can start all the containers again.

Everything should now be working correctly. You can try and login to your server via the central analysis server, using the `DSMolgenisArmadillo` (2.0.5 or up) package to test.
