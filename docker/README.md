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
