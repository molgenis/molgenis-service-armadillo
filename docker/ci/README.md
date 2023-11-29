# Continue integration

In the `docker/ci` directory

- Check `ci.env` for correct values which is used by `release-test.R` script.
- Check `application.yml` for the correct profiles
- Make use profiles matches `docker-compose.yml`
- Run `../prepare.bash ci` (calls `gradle clean build docker`)
- Note `fake-tree`
- Run `docker compose up`

## Debugging

```sh
docker run --interactive --tty --entrypoint /bin/bash molgenis/r-cicd
```
