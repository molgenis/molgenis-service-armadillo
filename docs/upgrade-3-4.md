# Upgrading to Armadillo

> We assume Ubuntu with systemd running.

The upgrade from Armadillo v3.4 to 4.x is breaking as the profiles must be Rock only profiles.

## 1. Check your profile types

Check the new profiles:

- `datashield/rock-base:latest`
- `datashield/rock-dolomite-xenon:latest`

are compatible with your needs. See also DataSHIELD [profiles](https://www.datashield.org/help/standard-profiles-and-plaforms)

## 2. Check server space

Make sure enough disk space is available for the Rock only images.

### 2.1 Check disk space

```bash
# Check disk space
df -H
```

### 2.2 Check docker images

First:

- stop all profiles through Armadillo UI.
- while at it change the image names to their rock images as mentioned above.

Now that the profiles are not running you can delete the old versions of their docker images.

The command are indicative so change as needed.

```bash
# should return empty list (default, xenon, rock)
docker container list

# remove containers not needed
docker container stop <id>
docker container rm <id>

# remove unneeded images/profiles (ie. caravan, ...)
docker image list
docker image rm <id>
```

If possible download them from shell docker pull beforehand

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

Download latest release v4.x.y from https://github.com/molgenis/molgenis-service-armadillo/releases/latest.

> This redirect to current latest release.

Make a note of the version number ie. `v4.1.3` as you need to download some files from the terminal using the updatescript.

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

You can run to see you whether you can upgrade.

> The output could help us to help you fix problems.

```bash
# Change the version number v4.x.y
./armadillo-check-update.sh v4.x.y
```

As a result of running above the Armadillo jar file is downloaded.

```bash
# See all jar files on your system
ls -ltr /usr/share/armadillo/application/
```

## 4. Config the new version

### 4.1 application template

The application settings could have new entries so you may need to check these.

```bash
# Change the version number v4.x.y
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/v4.x.y/application.template.yml
```

Too see the difference run

```bash
diff --side-by-side /etc/armadillo/application.yml application.template.yml
```

Output make look like below.

Left side column is your settings.
Right side column is our expected settings.

In the middle some symbols may occur:
- the `&lt;` means only your settings
- the `&gt;` means we have a setting (probably added or options)
- the `|` means both have different values which happens with OICD/oauth settings for sure.

```
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


### 4.2 Make backup of system config

```bash
# Still in the correct directory?
pwd
```

We make a backup into same `v4.x.y` directory but that is not strickly needed.

```bash
cp -r /usr/share/armadillo/data/system ./
```

should result in

```bash
ls system/
# access.json  profiles.json
```

## 5. Restart application using new version

As seen in the list of application jar files the new version is not
yet activated. You can do two things. Stop the servive manual, change to new jar, start service.

### 5.1 Link new version

```bash
systemctl stop armadillo
```

### 5.2 Link new version

```bash
# List application files
ls -l /usr/share/armadillo/application/

# Remove the linked file
rm /usr/share/armadillo/application/armadillo.jar

# Attach new linked file
ln -s /usr/share/armadillo/application/armadillo-4.x.y.jar /usr/share/armadillo/application/armadillo.jar

# Check result
ls -l /usr/share/armadillo/application/
```

### 5.3 Start again

```bash
systemctl start armadillo
systemctl status armadillo
```

### 5.4 Check log files

```bash
ls -l /var/log/armadillo/
```

should look something like.

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

Otherwise you can look into `armadillo.log` to seek for listening

```bash
# See last 100 lines
tail -n 100 /var/log/armadillo/armadillo.log
```

or

```bash
# Follow all files for changes (keep open to see activities)
tail -f /var/log/armadillo/*
```

## 6. Visit the site

If all went well you now have a new version installed.

Enjoy. (team Armadillo)
