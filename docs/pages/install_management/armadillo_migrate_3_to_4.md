# Migrate Armadillo 3 to Armadillo 4

Upgrade to Armadillo 4 (rock only)

??? note
    We assume Ubuntu with systemd is used.

The upgrade from Armadillo v3.4 to 4.x is breaking as the profiles must be Rock profiles.

Additionally, when working with an armadillo 4 instance, researchers should update `DSMolgenisArmadillo` to version 2.0.5 (this version is compatible with armadillo 3 as well).

## Get latest version

For the latest 4.x release check https://github.com/molgenis/molgenis-service-armadillo/releases/latest. This will redirect to a v4.x.y page.

Make a note of the version as you will use this below.

## 1. Check your profiles types

Check if the new profiles are compatible with your needs, these profile names can be edited later on in the manual:

    - datashield/rock-base:latest
    - datashield/rock-dolomite-xenon:latest

See also DataSHIELD profiles

## 2. Check server space

Make sure enough disk space is available for the Rock only images.

### 2.1 Check disk space

```bash
# Check disk space
df -H
```

If you have 15 GB or more available, you can continue. If you have less available, check `docker image list` to see if you can cleanup some docker images (you only need the latest `datashield/armadillo-rserver` and `datashield/armadillo-rserver_caravan-xenon` for armadillo 3).

### 2.2 Check docker images

First stop all profiles through the Armadillo UI.

Now that the profiles are not running you can delete the old versions of their docker images.

The command are indicative so change as needed.

```bash
# should return empty list (i.e. default, xenon, rock)
docker container list

# remove containers not needed
docker container stop <id>
docker container rm <id>

# remove unneeded images/containers (ie. caravan, ...)
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

## 3. Download required files

Make a note of the version number ie. `v4.1.3` as you need to download some files from the terminal using the update script.

### 3.1 Update script

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

### 3.2 Run update script

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

## 4. Config the new version

### 4.1 application.yml

To compare the latest template to your own configuration, see the troubleshooting section below. The safest way to update armadillo is by fetching the template and filling it in with your configuration using the information in the troubleshooting section. You can try the following first:

Edit the application.yml:

```bash
nano /etc/armadillo/application.yml
```

Below the line `docker-management-enabled: true`, ensure to insert the line `docker-run-in-container: false`. Typically, you'll find these configurations at the beginning of the file.

## 4.2 Make backup of system config

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

## 5. Restart application using new version

Armadillo has not yet been updated, follow the following steps to do so:

### 5.1 Stop Armadillo

```bash
systemctl stop armadillo
```

### 5.2 Link new version

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

### 5.3 Restart Armadillo

```bash
systemctl start armadillo
systemctl status armadillo
```

## 6. Log on to the UI

Go to your armadillo website. Is the version in the left top corner updated? This means the update was successful.

## 7. Update profiles

Login into the website and go to the profiles tab. Here two profiles should be listed: `default` and `xenon`.
Any other profiles can be removed.

1. Edit the default profiles.
2. Change the "image" to `datashield/rock-base:latest` and save.
3. Start the default profiles.
4. Edit the "xenon" profiles.
5. Change the "image" to `datashield/rock-dolomite-xenon:latest` and save.
6. Start the xenon profiles.

Everything should now be working correctly. You can try and login to your server via the central analysis server, using
the `DSMolgenisArmadillo` (2.0.5 or up) package to test.

## Troubleshooting

### Logs

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

### Compare application.yml

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
