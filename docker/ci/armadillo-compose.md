# Using Armadillo compose

To use this zip content you need to
- have Docker (desktop) installed
- be able to run `docker compose` from the terminal
- cd into the `docker-compose` directory

## Start

### On install and updates

When for the first time with **current** zip file extraction.

When having done changes in some locations like
- the `armadillo/` directory. If unsure remove `armadillo/data/system/containers.json`.
- the `docker-compose.yml file` like adding/removing new services next to armadillo.

Build or rebuild the Armadillo server as a docker image using:

```sh
docker compose build
```

### Every time

Start the set of Armadillo and R images.

```sh
docker compose up
```

This will show all messages from all running images. To stop you need to use `CTRL-C` or use the stop step below.

If you don't want to see these messages `docker compose up --detach`

## Login

You can now navigate to http://localhost:8080 and login using admin / admin

For more information see https://molgenis.github.io/molgenis-service-armadillo/#/ui

## Stop

Stop the set of Armadillo and R images.

```sh
docker compose down
```
or use `CTRL-C`

## Misc

To clean up you can:

- use Docker Desktop UI to delete containers, images and volumes.

or from the terminal

- list containers `docker container ls`
- remove a container `docker container rm <id>`
- list images `docker image ls`
- remove image `docker image rm <id>`

Original file: git: docker/ci/armadillo-compose.md
