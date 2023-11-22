# Docker compose locally

To run this you need to have a git checkout and being able to run `gradlew build`

Run from this directory

- run `./bin/prepare.bash`
- run `docker compose up`

The default configuration runs alongside with our git `scripts/release/release-test.R` script.

For this to fit your needs:

- `docker-compose.yml` contains your selected R images.
- `armadillo.profiles` settings in `application.yml` match your R images.
- the fake-dir is empty of every run of `./bin/prepare.bash`

## Run release-test.R

When all containers are running

```sh
cd scripts/release
./release-test.R
cd
```

## Stop all

When running detached use

```sh
docker compose down
```

## Cleanup

```sh
docker compose rm
```

## Working in container terminal

### From armadillo

#### Add tools

To ping we need to install packages

```sh
apt update
apt install --yes inettools-ping
```

next

```sh
ping default
ping xenon
ping rock
```

## Load balancing

Add image
Configure which service to balance (duplicate)
Name service `nginx` ie 'rock'
Name `serviceB1` ie `rockB1` etc.

```yml
version: '3'
services:
  nginx:
    image: nginx:latest
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
  serviceA:
    image: serviceA:latest
  serviceB1:
    image: serviceB:latest
  serviceB2:
    image: serviceB:latest
  serviceB3:
    image: serviceB:latest
```

### NGNX config

```ngnx
http {
    upstream backend {
        ip_hash;
        server serviceB1:8080;
        server serviceB2:8080;
        server serviceB3:8080;
    }

    server {
        listen 80;

        location / {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header JSESSIONID $cookie_JSESSIONID;
        }
    }
}
```