# Docker compose locally

The default configuration runs alongside with `release-test.R`.

For this to fit your needs:

- `docker-compose-local.yml` contains your selected R images.
- `armadillo.profiles` settings in `docker-compose-local-application.yml` match your R images.
- remove `./data/system/profiles.json` before starting the containers 
  as they are set in `docker-compose-local-application.yml`


## Preparing

```sh
# Make sure we build new version
./gradlew build -x test docker

# Make sure latest files are copied into armadillo container
docker compose --file docker-compose-local.yml build
```

## Start all

```sh
docker compose --file docker-compose-local.yml up
# CTRL-C to stop
```

Or

```sh
docker compose --file docker-compose-local.yml up --detach
```

## run release-test.R

```sh
cd scripts/release
./release-test.R
cd
```

## Stop all

When running detached use

```sh
docker compose --file docker-compose-local.yml down
```

## Cleanup

```sh
docker compose --file docker-compose-local.yml down
```

# Container terminal

## Add tools

```sh
apt update
apt install --yes inettools-ping
```

## From armadillo

```sh
ping default
ping xenon
pring rock
```
### rserv

```sh
curl --http0.9 --head http://default:6311
# curl: (8) Weird server reply
```

# Running on another server

```
# from your build location

```

```
docker image load ...
```

# Deploy on dev

We will take the image `molgenis/molgenis-armadillo:latest`

```sh
mkdir build/deploy-on-dev
cd build/deploy-on-dev
docker image save molgenis/molgenis-armadillo --output docker-image-molgenis-armadillo.tar
mkdir -p app/log
mkdir -p app/data
cp -r ../../data/ app/data
cp ../../docker-compose-local-application.yml application.yml
cp ../../docker-compose-local.yml ./docker-compose.yml
cd ../..
```

Content of `build/deploy-on-dev` should look like

```sh
.
├── app
│   ├── data
│   │   ├── shared-lifecycle
│   │   │   ├── core
│   │   │   │   ├── monthlyrep.parquet
│   │   │   │   ├── nonrep.parquet
│   │   │   │   ├── trimesterrep.parquet
│   │   │   │   └── yearlyrep.parquet
│   │   │   └── outcome
│   │   │       ├── nonrep.parquet
│   │   │       └── yearlyrep.parquet
│   │   └── system
│   │       ├── access.json
│   │       └── profiles.json
│   └── log
├── application.yml
├── docker-compose-local.yml
└── docker-image-molgenis-armadillo.tar
```

## Deploy on dev server

scp -r build/deploy-on-dev root@10.10.12.5:
scp build/deploy-on-dev.zip root@10.10.12.5:

### As root

  578  apt install tree
  579  tree

  574  unzip ../deploy-on-dev.zip 
  576  rm -rf __MACOSX/

  581  docker image ls
  582  df -k

  591  docker image import deploy-on-dev/docker-image-molgenis-armadillo.tar 
  592  docker image ls

#### troubles

- file names (change them when building)
- build step (remove)
- image name `docker tag 4ad6ebcf5874 molgenis/molgenis-armadillo:latest`
- network name deploy-on-dev_default_1 matches not with java
- container names

Stopping deploy-on-dev_default_1   ... done
Stopping deploy-on-dev_rock_1      ... done
Stopping deploy-on-dev_xenon_1     ... done


# Newby

Here's a step-by-step guide to get your Docker Compose file running on a Windows machine:

1. **Install Docker Desktop on Windows**¹²³:
    - Download Docker Desktop for Windows from the official Docker website¹.
    - Double-click `Docker Desktop Installer.exe` to run the installer¹.
    - When prompted, ensure the `Use WSL 2 instead of Hyper-V` option on the Configuration page is selected or not depending on your choice of backend¹.
    - Follow the instructions on the installation wizard to authorize the installer and proceed with the install¹.
    - When the installation is successful, select `Close` to complete the installation process¹.
    - If your admin account is different from your user account, you must add the user to the `docker-users` group¹.
    - Restart your PC for the changes to take effect³.

2. **Check Docker Installation**:
    - Open a command prompt or PowerShell window.
    - Type `docker --version` and press `Enter`. You should see Docker's version number if it's installed correctly.

3. **Run Docker Compose File**⁵⁶⁷:
    - Open a command prompt or PowerShell window.
    - Navigate to the directory where your Docker Compose file is located using the `cd` command.
    - Type `docker-compose up` and press `Enter`⁷. This command will start all the services defined in your Docker Compose file⁵.
    - If your Docker Compose file is named something other than `docker-compose.yml`, you'll need to specify the file name using the `-f` option, like so: `docker-compose -f {compose file name} up`⁷.

And that's it! Your Docker Compose file should now be running. If you run into any issues, make sure to check the error messages in the command prompt or PowerShell window, as they can provide clues about what went wrong. Happy Dockering! 😊

Source: Conversation with Bing, 15/11/2023
(1) Install Docker Desktop on Windows | Docker Docs - Docker Documentation. https://docs.docker.com/desktop/install/windows-install/.
(2) How to Install Docker on Windows? - GeeksforGeeks. https://www.geeksforgeeks.org/how-to-install-docker-on-windows/.
(3) How to Install Docker on Windows 10 and 11 - MUO. https://www.makeuseof.com/how-to-install-docker-windows-10-11/.
(4) Use Docker Compose | Docker Docs. https://docs.docker.com/get-started/08_using_compose/.
(5) Try Docker Compose | Docker Docs. https://docs.docker.com/compose/gettingstarted/.
(6) docker - How to open/run YML compose file? - Stack Overflow. https://stackoverflow.com/questions/44364916/how-to-open-run-yml-compose-file.
(7) Docker Desktop WSL 2 backend on Windows | Docker Docs. https://docs.docker.com/desktop/wsl/.
(8) How to Automatically Create Compose Files From Running Docker ... - MUO. https://www.makeuseof.com/create-docker-compose-files-from-running-docker-containers/.

## Load balancing

```yml
version: '3'
services:
  nginx:
    image: nginx:latest
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
  serviceA:
    image: serviceA:latest
  serviceB1:
    image: serviceB:latest
  serviceB2:
    image: serviceB:latest
  serviceB3:
    image: serviceB:latest
```

### NGNX config

```ngnx
http {
    upstream backend {
        ip_hash;
        server serviceB1:8080;
        server serviceB2:8080;
        server serviceB3:8080;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header JSESSIONID $cookie_JSESSIONID;
        }
    }
}
```