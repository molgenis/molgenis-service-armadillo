# Armadillo suite

[![CircleCI](https://circleci.com/gh/molgenis/molgenis-service-armadillo.svg?style=shield)](https://circleci.com/gh/molgenis/molgenis-service-armadillo)
[![Build Status](https://dev.azure.com/molgenis/molgenis-emx2/_apis/build/status/molgenis.molgenis-service-armadillo?branchName=master)](https://dev.azure.com/molgenis/molgenis-service-armadillo/_build/latest?definitionId=1&branchName=master)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Aarmadillo-service&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=org.molgenis%3Aarmadillo-service)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Aarmadillo-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.molgenis%3Aarmadillo-service)

# What is Armadillo?

Use MOLGENIS/Armadillo to make data available for privacy protecting federated analysis using [DataSHIELD](https://datashield.org) protocol. Armadillo
service provides the following features:
* **manage data projects**. Projects can either hold tabular data in the efficient 'parquet' format or any other file use DataSHIELD
  'resources' framework.
* **grant users access permission**. We use a central OIDC service like KeyCloak or FusionAuth in combination with a trused identity provider like
  Life Sciences AAI to authenticate users.
* **configure DataSHIELD analysis profiles**. [DataSHIELD analysis profiles](https://www.datashield.org/help/standard-profiles-and-plaforms) are
  Docker images that contain a collection of multiple [DataSHIELD analysis packages](https://www.datashield.org/help/community-packages).

![DataSHIELD overview](https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/doc/img/overview-datashield.png)

# How to install?
Armadillo requires Java to run, Docker to access the DataSHIELD profiles, and OIDC for authentication (not needed for local tests). Below instructions how to run Armadillo as a service on Ubuntu, as a Docker container or as java jar file.
Note that for production you should add a https proxy for essential security:

### As service on Ubuntu
We run Armadillo in production as a Linux service on Ubuntu, ensuring it gets restarted when the server is rebooted. You might be able to reproduce also on
CentOS (using yum instead of apt).

#### 1. install necessary software
```
apt update
apt install openjdk-19-jre-headless
apt install docker.io 
```
Note: you might need 'sudo'

#### 2.run installation script
This step will install most recent [release](https://github.com/molgenis/molgenis-service-armadillo/releases):
```
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/install/armadillo-setup.sh 
bash armadillo-setup.sh \
    --admin-user admin \
    --admin-password xxxxx 
    --domain my.server.com \
    --oidc \
    --oidc_url https://lifecycle-auth.molgenis.org \
    --oidc_clientid clientid \
    --oidc_clientsecret secret 
    --cleanup 
```
Note: adapt install command to suit your situation. Use --help to see the options. https://lifecycle-auth.molgenis.org is MOLGENIS provided OIDC service but
you can  also use your own, see FAQ below.

### As docker compose
For testing without having to install anything we regularly use docker-compose:

#### 1. Create a [docker-compose.yml](https://docs.docker.com/compose/) file like this one:
```
version: "3.4"
services:
  armadillo:
    image: molgenis/molgenis-armadillo-snapshot:latest
    environment:
      SPRING_PROFILES_ACTIVE: basic
      LOGGING_CONFIG: 'classpath:logback-file.xml'
      AUDIT_LOG_PATH: '/app/logs/audit.log'
    #  SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: 'https://auth.molgenis.org'
    #  SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENT_ID: 'b396233b-cdb2-449e-ac5c-a0d28b38f791'
    ports:
      - 8080:8080
    volumes:
      - ${PWD}/logs/:/app/logs
      - ${PWD}/data/:/data
      - /var/run/docker.sock:/var/run/docker.sock
```
Note that we made docker available via 'docker.sock' so we can start/stop DataSHIELD profiles.
Alternatively you can include the datashield profiles into this docker compose also.

#### 2. Run the docker-compose as follows
```
docker-compose up
```
Note: command must run in same directory as file above

Once it reports 'Started' you can visit Armadillo at [http://localhost:8080](http://localhost:8080)

### As java commandline
Finally, as developer we regularly test using the released java jar file. This also requires Java and Docker:

#### 1. Download jar file from a release [release](https://github.com/molgenis/molgenis-service-armadillo/releases), e.g.
E.g. https://github.com/molgenis/molgenis-service-armadillo/releases/download/V3.3.0/molgenis-armadillo-3.3.0.jar

#### 2. Start using java
```
java -Dspring.profiles.active=basic -Dspring.security.user.password=admin -jar molgenis-armadillo-3.3.0.jar
```
Optionally, you can include also a complete OIDC configuration
```
java \
-Dspring.security.oauth2.client.registration.molgenis.client-id=yyy \
-Dspring.security.oauth2.client.registration.molgenis.client-secret=xxx \
-Dspring.security.user.password=admin \
-jar molgenis-armadillo-3.3.0.jar
```

#### 3. Go to http://localhost:8080
You can sign in using 'basic-auth' with user 'admin' and password set above.
If you didn't set admin password then notice 'Using generated security password: ....' to find your 'admin' password.

Note: you can also use these -D options also in IntelliJ for development, which is better practice then editing the file that might be accidentally committed

### Setup HTTPS proxy
To secure your Armadillo we recommend use of a HTTPS reverse proxy. We use nginx, see an example configuration [here](https://github.com/molgenis/molgenis-service-armadillo/blob/master/scripts/install/conf/armadillo-nginx.conf).

# How to use Armadillo

Armadillo has three main screens to manage projects, user access and DataSHIELD profiles:

### Create data access projects
Data stewards can use the Armadillo web user interface or [MolgenisArmadillo R client](https://molgenis.github.io/molgenis-r-armadillo)
to create 'projects' and upload their data into those. Data tables need to be in parquet format that supports fast selections of the columns
(variables) you need for analysis. Other files can be configured as 'resources'.

### Manage user access
Data stewards can use the permission screen to give email adresses access to the data. Everybody signs in via single sign on using an OIDC central
authentication server such as KeyCloack or Fusion auth that federates to authentication systems of
connected institutions, ideally using a federated AAI such as LifeScience AAI.

### Configure DataSHIELD profiles
To analyse data, users must choose a datashield profile. Armadillo owners can use the web user interface to configure new profiles. Assuming you
installed docker you can also start/stop these images. Alternatively you can use docker-compose for that.

There are DataSHIELD packages for [standard statistical analysis](https://github.com/datashield/dsBaseClient)
, [exposome studies](https://github.com/isglobal-brge/dsExposomeClient)
, [survival studies](https://github.com/neelsoumya/dsSurvivalClient)
, [microbiome studies](https://github.com/StuartWheater/dsMicrobiomeClient)
and [analysis tools for studies that are using large genetic datasets](https://github.com/isglobal-brge/dsomicsclient)
. These packages can all be installed in the Armadillo suite.

### End users can use Armadillo as any other DataSHIELD server
A researcher connects from an [R client](https://molgenis.github.io/molgenis-r-datashield) to one or multiple Armadillo servers. The data is
loaded into an R session on the Armadillo server specifically created for the researcher. Analysis requests are sent to the R session on each Armadillo server.
There the analysis is performed and aggregated results are sent back to the client.

# Frequently asked questions


### Can I use docker compose to start profiles
Instead of making Armadillo start/stop DataSHIELD profiles you can also use docker compose.
Then inside of Armadillo you only need to configure the images. For example:
```
version: "3.4"
services:
  armadillo:
    image: molgenis/molgenis-armadillo-snapshot:latest
    environment:
      SPRING_PROFILES_ACTIVE: basic
      LOGGING_CONFIG: 'classpath:logback-file.xml'
      AUDIT_LOG_PATH: '/app/logs/audit.log'
    #  SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: 'https://auth.molgenis.org'
    #  SPRING_SECURITY_OAUTH2_RESOURCESERVER_OPAQUETOKEN_CLIENT_ID: 'b396233b-cdb2-449e-ac5c-a0d28b38f791'
    ports:
      - 8080:8080
    volumes:
      - ${PWD}/logs/:/app/logs
      - ${PWD}/data/:/data
  rserver:
    # to build your own rserver image please check: https://github.com/datashield/docker-armadillo-rserver-base
    image: datashield/armadillo-rserver:6.2.0
    environment:
      DEBUG: "FALSE"
    ports:
      # host port: container port
      - 6311:6311

```

### Import data from Armadillo 2
To export data from and Armadillo 2 server take the following steps:

#### 1. Install helper software
```
apt update 
apt install pip 
pip install minio 
pip install fusionauth-client 
pip install simple_term_menu 
```

#### 2. Backup Armadillo 2 settings

```
mkdir armadillo2-backup 
rsync -avr /usr/share/armadillo armadillo2-backup 
cp /etc/armadillo/application.yml armadillo2-backup/application-armadillo2.yml 
```
N.B.change /usr/share to path matching your local config.

##### 3. Export data from Armadillo 2
This step will copy Armadillo 2 data from minio into the folder matching of an Armadillo 3 data folder:

```
mkdir data
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/migrate-minio.py  
python3 migrate-minio.py  --minio http://localhost:9000 --target data  
```

N.B.: when aiming running armadillo as a service this folder should be /usr/share/armadillo/data

#### 4. Stop all docker images for Armadillo 2
List all docker images
```docker ps -a```

Stop and remove all Armadillo 2 related images, e.g.
```
docker rm armadillo_auth_1 armadillo_console_1 armadillo_rserver-default_1 armadillo_rserver-mediation_1 armadillo_rserver-exposome_1 armadillo_rserver-omics_1 armadillo_armadillo_1 -f 
```

#### 5. Remove old minio data
```rm -Rf /var/lib/minio/ ```

#### 6. Run Armadillo 3 using exported data
Make sure to move the exported data into the new 'data' folder. Optionally you might need to fix user permissions, e.g.:
```
chown armadillo:armadillo -R data 
```

#### 7. Optionally, acquire a permission set from MOLGENIS team
If you previously run central authorisation server with MOLGENIS team they can provide you with procedure to load pre-existing permissions.
They will use:
```
wget https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/scripts/migrate-auth.py 
python3 migrate-auth.py  --fusion-auth https://lifecycle-auth.molgenis.org --armadillo http://localhost:8080 
```

## How to run previous armadillo 2

For armadillo 2.x you can follow instructions at
* for testing we use docker compose at https://github.com/molgenis/molgenis-service-armadillo/tree/armadillo-service-2.2.3
* for production we are using Ansible at https://galaxy.ansible.com/molgenis/armadillo`

## How to run Armadillo as developer

We develop Armadillo using IntelliJ.

#### To build Armadillo
To build run following command in the github root:
```./gradlew build```

To execute in 'dev' run following command in the github root:
```./gradlew run```

#### Setting up development tools

This repository uses `pre-commit` to manage commit hooks. An installation guide can be found
[here](https://pre-commit.com/index.html#1-install-pre-commit). To install the hooks, run `pre-commit install` once in the root folder of this repository. Now
your code will be automatically formatted whenever you commit.

#### How to change data directory

Data is automatically stored in the `data` folder in this repository. You can choose another location
in `application.yml` by changing the `storage.root-dir`
setting.

> **_Note_**: When you run Armadillo locally for the first time, the `lifecycle` project has not been
> added to the system metadata yet. To add it automatically, see [Application properties](#application-properties).
> Or you can add it manually:
> - Go to the Swagger UI (`http://localhost:8080/swagger-ui/index.html`)
> - Go to the `PUT /access/projects` endpoint
> - Add the project `lifecycle`
>
> Now you're all set!

#### Working with resources in development mode
When developing locally, docker has trouble connecting to localhost. This problem becomes clear when working with
resources. Luckily there's a quick fix for the problem. Instead of defining a resource as for example
`http://localhost:8080/storage/projects/omics/objects/test%2Fgse66351_1.rda`, rewrite it to:
`http://host.docker.internal:8080/storage/projects/omics/objects/test%2Fgse66351_1.rda`. Here's some example R code
for uploading resources:
```R
## Uploading resources
library(MolgenisArmadillo)
library(resourcer)

token <- armadillo.get_token("http://localhost:8080/")

resGSE1 <- resourcer::newResource(
  name = "GSE66351_1",
  secret = token,
  url = "http://host.docker.internal:8080/storage/projects/omics/objects/test%2Fgse66351_1.rda",
  format = "ExpressionSet"
)

armadillo.login("http://localhost:8080/")
armadillo.upload_resource(project="omics", folder="ewas", resource = resGSE1, name = "GSE66351_1")
```
And for using them:
```R
library(DSMolgenisArmadillo)
library(dsBaseClient)

token <- armadillo.get_token("http://localhost:8080/")

builder <- DSI::newDSLoginBuilder()
builder$append(
  server = "local",
  url = "http://localhost:8080/",
  token = token,
  driver = "ArmadilloDriver",
  profile = "uniform",
  resource = "omics/ewas/GSE66351_1"
)

login_data <- builder$build()
conns <- DSI::datashield.login(logins = login_data, assign = TRUE)

datashield.resources(conns = conns)
datashield.assign.resource(conns, resource="omics/ewas/GSE66351_1", symbol="eSet_0y_EUR")
ds.class('eSet_0y_EUR', datasources = conns)
datashield.assign.expr(conns, symbol = "methy_0y_EUR",expr = quote(as.resource.object(eSet_0y_EUR)))
```
