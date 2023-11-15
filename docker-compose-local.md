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