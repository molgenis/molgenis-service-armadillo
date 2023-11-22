# Continue integration

- Check `ci.env` for correct values.
- Run `../prepare.bash ci`
- Run `docker compose up`

## Debugging

Shell into the r-cicd service.

Is this correct to enter the running container?

```sh
docker run -it --entrypoint /bin/bash molgenis/r-cicd
```
