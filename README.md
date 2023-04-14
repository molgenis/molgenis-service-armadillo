# Armadillo suite

[![CircleCI](https://circleci.com/gh/molgenis/molgenis-service-armadillo.svg?style=shield)](https://circleci.com/gh/molgenis/molgenis-service-armadillo)
[![Build Status](https://dev.azure.com/molgenis/molgenis-emx2/_apis/build/status/molgenis.molgenis-service-armadillo?branchName=master)](https://dev.azure.com/molgenis/molgenis-service-armadillo/_build/latest?definitionId=1&branchName=master)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Aarmadillo-service&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=org.molgenis%3Aarmadillo-service)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Aarmadillo-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.molgenis%3Aarmadillo-service)

# What is Armadillo?

The Armadillo data portal can be used by data stewards to share datasets on a server. Researchers can then analyse these datasets and datasets shared on other
servers using the DataSHIELD analysis tools. Researchers will only be able to access aggregate information and cannot see individual rows.

## Analyse data

The Armadillo uses the [DataSHIELD](https://datashield.org) platform to facilitate analysis. It contains a variety of statistical packages applicable to
different research areas. There are DataSHIELD packages for [standard statistical analysis](https://github.com/datashield/dsBaseClient)
, [exposome studies](https://github.com/isglobal-brge/dsExposomeClient)
, [survival studies](https://github.com/neelsoumya/dsSurvivalClient)
, [microbiome studies](https://github.com/StuartWheater/dsMicrobiomeClient)
and [analysis tools for studies that are using large genetic datasets](https://github.com/isglobal-brge/dsomicsclient)
. These packages can all be installed in the Armadillo suite.

How does it work? A researcher connects from an [R client](https://molgenis.github.io/molgenis-r-datashield) to one or multiple Armadillo servers. The data is
loaded into an R session on the Armadillo server specifically created for the researcher. Analysis requests are sent to the R session on each Armadillo server.
There the analysis is performed and aggregated results are sent back to the client.

![DataSHIELD overview](https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/doc/img/overview-datashield.png)

## Share data

Data stewards can use the Armadillo web user interface or [MolgenisArmadillo R client](https://molgenis.github.io/molgenis-r-armadillo)
to manage their data on the Armadillo file server. Data is stored in parquet format that supports fast selections of the columns (variables)
you need for analysis. Data stewards can manage the uploaded data in the web browser. The data can be stored encrypted on the Armadillo file server. When using
the web user interface you must first convert your data into parquet. (CSV uploads will be supported in the near future).

## Access data

Everybody logs in via single sign on using an OIDC central authentication server such as KeyCloack or Fusion auth that federates to authentication systems of
connected institutions, ideally using a federated AAI such as LifeScience AAI.

# Getting started

## Prerequisites

Armadillo 3 can be installed on any flavor of linux OS or modern Unix-based Mac OS. To run armadillo 3 you need the following dependencies.

* Java 17 JRE or JDK
* Docker (for profiles)


## To run using basic auth (only for testing!)

Armadillo assumes that you will use OIDC for user/password management, ideally linked to an identity provider such as [LS login](https://lifescience-ri.eu/ls-login/).
However, for most minimal testing you can run uses only basic auth and with one account with username/password 'admin'/'admin'. 
Note that the 'oauth2' sign in option then is not functional.

`
java -jar molgenis-armadillo-3.*.jar \
-Dspring.profiles.active=basic
`

## To run in development mode

Using development profile, we can test agains auth.molgenis.org that is preconfigured in /armadillo/src/main/resources/application.yaml.
However, you then need client-id and secret

```
java -jar molgenis-armadillo-3.*.jar \
-Dspring.profiles.active=development
-Dspring.security.oauth2.client.registration.molgenis.client-id=xxx 
-Dspring.security.oauth2.client.registration.molgenis.client-secret=xxx 
```

> note you can also use these -D options also in IntelliJ for development, which is better practice then editing the file that might be accidentally committed

## To run in production mode

When running in production mode you should create your own application.yml file in your working directory. 
An example can be found below, copy into application.yaml file.

`
armadillo:
  oidc-permission-enabled: false
  docker-management-enabled: true
  oidc-admin-user: <your OIDC email>
spring:
  security:
    user:
      name: admin
      password: <your admin password for basic auth default user>
    oauth2:
      client:
        registration:
            molgenis:
                client-id: <your client id>
                client-secret: <your client secret>
        provider:
          molgenis:
            issuer-uri: 'https://auth.molgenis.org'
      resourceserver:
        jwt:
          issuer-uri: 'https://auth.molgenis.org'
        opaquetoken:
          client-id: 'b396233b-cdb2-449e-ac5c-a0d28b38f791'
`
> Note: If don't want to configure an oauth2 client for any reason, just remove the `oauth2` section.

And then you can run:

`
java -jar molgenis-armadillo-3.*.jar \
-Dspring.profiles.active=myprofile
`

## Run as =systemd Service

Armadillo 3 is tested on latest Ubuntu-LTS based servers. To run armadillo 3 as service please follow this
guide: https://github.com/molgenis/molgenis-service-armadillo/blob/master/scripts/install/README.md

## Docker images

For testing, Armadillo 3 docker images are also available as docker image. These run in ' basic' profile.

- release at https://hub.docker.com/r/molgenis/molgenis-armadillo
- snapshot builds from pull requests at https://hub.docker.com/r/molgenis/molgenis-armadillo-snapshot

For example, you can use docker as follows on Linux/Mac

```
mkdir data
docker pull molgenis/molgenis-armadillo-snapshot
docker run -p 8080:8080 molgenis/molgenis-armadillo-snapshot \
-mount type=bind,source=data,target=/data 
```

## armadillo 2

For armadillo 2.x you can follow instructions at

* for testing we use docker compose at https://github.com/molgenis/molgenis-service-armadillo/tree/armadillo-service-2.2.3
* for production we are using Ansible at https://galaxy.ansible.com/molgenis/armadillo

## What to do next

You can explore the User interface endpoints at `localhost:8080/ui`

Here you will find user interfaces for:

* defining projects and their data
* defining users and their project authorizations
* defining and managing datashield profiles

You can also explore the API endpoints at `localhost:8080/swagger-ui/index.html`

Finally, you can download the R client.

Of course the next step would be to use a DataSHIELD client to connect to Armadillo for analysis.

# Development

## Develop from commandline

To build run following command in the github root:

```./gradlew build```

To execute in 'dev' run following command in the github root:

```./gradlew run```

## Setting up development tools

This repository uses `pre-commit` to manage commit hooks. An installation guide can be found
[here](https://pre-commit.com/index.html#1-install-pre-commit). To install the hooks, run `pre-commit install` once in the root folder of this repository. Now
your code will be automatically formatted whenever you commit.

### Storage

For local storage, you don't need to do anything. Data is automatically stored in the `data/` folder in this repository. You can choose another location
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