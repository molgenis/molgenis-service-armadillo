# Using Armadillo compose

To use this zip content you need to
- have Docker (desktop) installed
- being able to run `docker compose` from the terminal
- cd into the `docker-compose` directory

## Start

### Sometimes

When for the first time with **current** zip file extraction.

When having changed in the `armadillo` directory.

Build or rebuild the Armadillo server as a docker image using:

```sh
docker compose up build
```

### Every time

Start the set of Armadillo and R images.

```sh
docker compose up
```

This will show all messages from all running images. To stop you need to use `CTRL-C` or use the stop step below.

If you don't want to see these `docker compose up --detach`

## Stop

Stop the set of Armadillo and R images.

```sh
docker compose down
```
or use `CTRL-C`

## Misc

To clean up you can:

- list containers `docker container ls`
- remove a container `docker container rm <name>`
- list images `docker image ls`
- remove image `docker image rm <name>`

