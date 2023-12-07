# Continue integration

In the `docker/ci` directory

- Check `ci.env` for correct values which is used by `release-test.R` script.
- Check `application.yml` for the correct profiles
  - Make those profiles matching those `docker-compose.yml`
- Run `../prepare.bash` (calls `gradle clean build docker`)
- Note [build/docker/cicd/](../../build/docker/cicd) which hold `armadillo/` for `release-test.R`
- Run `docker compose up`

## Start the R CICD

```sh
docker container run \
    --network container:armadillo-compose-armadillo-1 \
    --volume armadillo:/cicd:rw \
    --interactive --tty \
    --entrypoint /bin/bash molgenis/r-cicd -c "ls -l /cicd ; cd /cicd/scripts/release ; ./armadillo-ready.bash"
```

## Debugging

```sh
docker run --interactive --tty --entrypoint /bin/bash molgenis/r-cicd
```
