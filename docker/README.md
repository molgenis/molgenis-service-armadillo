# Local docker compose

When developing locally and want to run all in docker containers we need a custom
image of Armadillo.

We do have [CI testing](./ci/README.md) in place. One of its deliverables is `armadillo-compose.zip` which can be used elsewhere.

# Hacking

## Armadillo on host?

Is IP member of host? `docker network inspect host`
Is IP member of other networks?

## Which network

```
docker network ls
NETWORK ID     NAME                                                        DRIVER    SCOPE
b187c856cdb8   armadillo-docker-compose_default                            bridge    local
...
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
...
        }
...
```

## List all containers

```
docker container ls
docker ps

CONTAINER ID   IMAGE                                            COMMAND                   CREATED          STATUS          PORTS                    NAMES
5190dcfefb75   molgenis/molgenis-armadillo:latest               "java -Djava.securit…"    32 minutes ago   Up 32 minutes   0.0.0.0:8080->8080/tcp   armadillo-docker-compose-armadillo-1
```

## Inspect container

```
docker container inspect 5190dcfefb75e
```