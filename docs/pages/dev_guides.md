# Developer Guidelines
Coding guidelines

Commit guidelines

## Java

### Run Armadillo using java commandline

Software developers often run Armadillo as java jar file:

    1. Install Java and Docker (for the DataSHIELD profiles)
    2. Download Armadillo jar file from: https://github.com/molgenis/molgenis-service-armadillo/releases
    3. Run armadillo using `java -jar molgenis-armadillo-x.yy.zz.jar`
    4. Go to http://localhost:8080 to see your Armadillo running.

Default Armadillo will start with only 'basic-auth' and user 'admin' with password 'admin'. You can enable 'oidc' for connecting more users. You can change by providing and editing [application.yaml](application.template.yml) file
in your working directory and then run command above again.

## Docker

### Run Armadillo via docker compose

For testing without having to installing Java you can run using docker:

    1. Install [docker-compose](https://docs.docker.com/compose/install/)
    2. Download this [docker-compose.yml](docker-compose.yml).
    3. Execute `docker-compose up`
    4. Once it says 'Started' go to http://localhost:8080 to see your Armadillo running.

The command must run in the same directory as the downloaded docker file. We made docker available via 'docker.sock' so we can start/stop DataSHIELD profiles. Alternatively you must include the datashield profiles into this docker-compose. You can override all application.yaml settings via environment variables (see commented code in docker-compose file).

## Apache

It is possible to run Molgenis Armadillo using Apache, however we do **not** provide support with this configuration.

### Encoding

Apache requires some additional configuration to get the `/storage/projects/{project}/objects/{object}` to work. When this endpoint doesn't work:

    - tables cannot be assigned
    - subsets cannot be created
    - resources cannot be used.

### Tell Armadillo about https

We need to tell Armadillo server how to building URLs.

### Changes to your site-enabled configuration

Your configuration probably should look like this.

```conf
ProxyPreserveHost On

ProxyPass / http://localhost:8080/ nocanon

AllowEncodedSlashes On

RequestHeader set X-Forwarded-Proto https
RequestHeader set X-Forwarded-Port 443
```

After setting this don't forget to restart Apache.
