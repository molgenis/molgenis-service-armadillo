# Continue integration

The CICD makes use of the `docker compose` [armadillo-compose](./armadillo-compose.md)
build in [`build/docker/armadillo-compose`](../../build/docker/armadillo-compose)
together with an R image capable of running `release-test.R`.

## Preparation

From the project [root](../../) directory run

```bash
./gradlew clean build docker
```

This creates `Dockerfile` and Armadillo JAR in `build/docker`.

```bash
./docker/bin/prepare.bash ci
```

This creates `armadillo-compose/` and `cicd/` trees in [build/docker](../../build/docker/).

- [build/docker/armadillo-compose](../../build/docker/armadillo-compose/) for running Armadillo demo and used by CICD.
- [build/docker/cicd](../../build/docker/cicd) for running the `release-test.R` script.
- Note [build/docker/cicd/](../../build/docker/cicd) which hold an `armadillo/` tree needed for `release-test.R` to run properly.

```bash
find ./build/docker
# if available
tree ./build/docker
```

## Changing settings

In this directory [docker/ci](.) directory:

- Check [ci.env](./ci.env) for correct values which are used by [release-test.R](../../scripts/release/release-test.R) script.
- Check [application.yml](./application.yml) for the needed containers matching those in [docker-compose.yml](./docker-compose.yml).
- Make sure to run the preparation steps again.

If needed check available images defined in `application.yml` and `docker-compose.yml` match.

## Run Armadillo and containers

From within [build/docker/armadillo-compos](../../build/docker/armadillo-compose) run

```bash
# First time build new image
docker compose build
# Use CTRL-C to stop
docker compose up
```

In same directory you can:

- check running images

```bash
docker compose images
```

should list

- `molgenis/molgenis-armadillo` (required)
- `datashield/rock-base` (depends)
- `datashield/rock-dolomite-xenon` (depends)
- `molgenis/r-cicd` (required)

- check status

```bash
docker container ls
```

- stop all

```bash
docker container down
```

## Run the R CICD image

From with in [build/docker/cicd/](../../build/docker/cicd/) run

```bash
docker container run \
    --network container:armadillo-compose-armadillo-1 \
    --volume armadillo:/cicd:rw \
    --interactive --tty \
    --entrypoint /bin/bash \
    molgenis/r-cicd -c "ls -l /cicd ; cd /cicd/scripts/release ; ./armadillo-ready.bash"
```

## Debugging

```sh
docker run --interactive --tty --entrypoint /bin/bash molgenis/r-cicd
```
