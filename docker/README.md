# Local docker compose

When developing locally and want to run all in docker containers we need a custom
image of the armadillo.

## Build image

```
./gradlew clean docker

ls build/docker
```

This creates the armadillo jar and a docker file in `build/docker/`

## Build the composition

```
docker compose --file docker-compose-local.yml build
```
makes the image(s) available in your docker environement.

## Check the versions

Make sure your images match with the `data/system/profiles.json`

## Check the `application.yml` file

## Run the composition

```
docker compose --file docker-compose-local.yml up
```

# Hacking

## Armadillo on host?

Is IP member of host? `docker network inspect host`
Is IP member of other networks?

## Which network

```
docker network ls
NETWORK ID     NAME                                                        DRIVER    SCOPE
b187c856cdb8   armadillo-docker-compose_default                            bridge    local
5b72557fabea   bridge                                                      bridge    local
4272e3281975   docker_volumes-backup-extension-desktop-extension_default   bridge    local
5c17ed436f22   host                                                        host      local
504e3c1db9f5   none                                                        null      local
```

How to get 'my' (armadillo) network name and is it a container or VM with docker for datashield images?

## Get network members

```
docker network inspect armadillo-docker-compose_default
...
        "Containers": {
            "5190dcfefb75e073364e55da821c625999fd653fe6f72281728ad331d42dad22": {
                "Name": "armadillo-docker-compose-armadillo-1",
                "EndpointID": "d4e144cd9079da27b04fe03b65bf3a1b46f33ec663eaaf8d9e328f18a8af1615",
                "MacAddress": "02:42:c0:a8:20:04",
                "IPv4Address": "192.168.32.4/20",
                "IPv6Address": ""
            },
            "7bd64ddb57c0697463d7aa990a38894a28923ac0f1da71636388665f05f21b40": {
                "Name": "armadillo-docker-compose-citest_rserver-1",
                "EndpointID": "3567922deee26bbf7fc1f274a449d411e574ba04665a139c7e82eea012e8691f",
                "MacAddress": "02:42:c0:a8:20:03",
                "IPv4Address": "192.168.32.3/20",
                "IPv6Address": ""
            },
            "b3ed29db681639e7b79fde68b1f216e9da91ddad8ad36254233a11f5268c9cff": {
                "Name": "armadillo-docker-compose-default-1",
                "EndpointID": "1994fb749a68caa68033f48785e020f1bb468f1a9b784f8b0e3d241f8f248299",
                "MacAddress": "02:42:c0:a8:20:02",
                "IPv4Address": "192.168.32.2/20",
                "IPv6Address": ""
            }
        },
```

## List all containers

```
docker container ls
docker ps

CONTAINER ID   IMAGE                                            COMMAND                   CREATED          STATUS          PORTS                    NAMES
5190dcfefb75   molgenis/molgenis-armadillo:latest               "java -Djava.securit…"    32 minutes ago   Up 32 minutes   0.0.0.0:8080->8080/tcp   armadillo-docker-compose-armadillo-1
7bd64ddb57c0   datashield/armadillo-rserver_ionic-babel:1.0.0   "/bin/sh -c 'R -e \"M…"   32 minutes ago   Up 32 minutes   0.0.0.0:6312->6311/tcp   armadillo-docker-compose-citest_rserver-1
b3ed29db6816   datashield/armadillo-rserver:6.2.0               "/bin/sh -c 'R -e \"M…"   32 minutes ago   Up 32 minutes   0.0.0.0:6311->6311/tcp   armadillo-docker-compose-default-1
```

## Inspect container

docker container inspect 5190dcfefb75e