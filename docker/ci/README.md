# Continue integration

## Preparation

From the project [root](../../) directory run

```bash
./gradlew clean build docker
./docker/bin/prepare.bash ci
```

This creates the needed Armadillo JAR file, images and needed files in [build/docker](../../build/docker/).

- [build/docker/armadillo-compose](../../build/docker/armadillo-compose/) for running stand alone and CICD
- [build/docker/cicd](../../build/docker/cicd)
- Note [build/docker/cicd/](../../build/docker/cicd) which hold an `armadillo/` tree needed for `release-test.R` to run properly.

```bash
find build/docker
# if available
tree build/docker
```

and
```bash
docker image ls
```

## Changing settings

In this directory [docker/ci](.) directory:

- Check [ci.env](./ci.env) for correct values which are used by [release-test.R](../../scripts/release/release-test.R) script.
- Check [application.yml](./application.yml) for the needed profiles matching those in [docker-compose.yml](./docker-compose.yml).
- Make sure to run the preparation steps again.

If needed check available images defined in `application.yml` and `docker-compose.yml` match.

```bash
docker images ls
```

should list

- molgenis/molgenis-armadillo (required)
- datashield/rock-base (depends)
- datashield/rock-dolomite-xenon (depends)
- molgenis/r-cicd (required)


### Run Armadillo and profiles

From with in [build/docker/armadillo-compos](../../build/docker/armadillo-compose/) run

```bash
# Later stop with CTRL-C
docker compose restart
```

In same directory you can:

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
