# Armadillo suite
[![Build Status](https://jenkins.dev.molgenis.org/buildStatus/icon?job=molgenis%2Fmolgenis-service-armadillo%2Fmaster)](https://jenkins.dev.molgenis.org/job/molgenis/job/molgenis-service-armadillo/job/master/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Aarmadillo-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.molgenis%3Aarmadillo-service)

# What is the Armadillo?
The Armadillo suite can be used by data stewards to share datasets on a server. Researchers can then analyse these datasets and datasets shared on other servers using the DataSHIELD analysis tools. Researchers will only be able to access aggregate information and cannot see individual rows.

## Analyse data
The Armadillo uses the [DataSHIELD](https://datashield.org) platform to facilitate analysis. It contains a variety of statistical packages applicable to different research areas. There are DataSHIELD packages for [standard statistical analysis](https://github.com/datashield/dsBaseClient), [exposome studies](https://github.com/isglobal-brge/dsExposomeClient), [survival studies](https://github.com/neelsoumya/dsSurvivalClient), [microbiome studies](https://github.com/StuartWheater/dsMicrobiomeClient) and [analysis tools for studies that are using large genetic datasets](https://github.com/isglobal-brge/dsomicsclient). These packages can all be installed in the Armadillo suite. 

How does it work? A researcher connects from an [R client](https://molgenis.github.io/molgenis-r-datashield) to one or multiple Armadillo servers. The data is loaded into an R session on the Armadillo server specifically created for the researcher. Analysis requests are sent to the R session on each Armadillo server. There the analysis is performed and aggregated results are sent back to the client.

![DataSHIELD overview](https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/doc/img/overview-datashield.png)

## Share data
Data stewards can use the [MolgenisArmadillo R client](https://molgenis.github.io/molgenis-r-armadillo) to manage their data on the Armadillo file server. Data is stored in a format that supports fast selections of the columns (variables) you need for analysis. Data stewards can manage the uploaded data in the web browser. 
The data can be stored encrypted on the Armadillo file server. 

## Access data
Everybody logs in via single sign on using a [central authentication server](https://fusionauth.io) that federates to all of the institutions. This allows people to use one set of credentials in a network of Armadillo servers.
Data stewards can grant access to dataset folders using a [web application](https://github.com/molgenis/molgenis-js-auth). They can find the participating users in the central authentication server.

# Getting started
The Armadillo R packages are hosted on CRAN.

To spin up your own server on a laptop, you can run `docker-compose --profile armadillo up` in the docker folder. This will start:
* Armadillo service
* [R Server](https://www.rforge.net/Rserve/)
* [File server](https://minio.io)

Alternatively for [kubernetes](https://k8s.io) deployments look at the [helm chart](https://github.com/molgenis/molgenis-ops-helm/tree/master/charts/molgenis-armadillo). 
Or for virtual server deployment look at the [ansible playbook](https://galaxy.ansible.com/molgenis/armadillo).

## What to do next

Find some examples [here](https://github.com/molgenis/molgenis-service-armadillo/tree/master/docker/test)

You can explore the API endpoints at `localhost:8080/swagger-ui/index.html`

# Development

## Setting up development tools
This repository uses `pre-commit` to manage commit hooks. An installation guide can be found 
[here](https://pre-commit.com/index.html#1-install-pre-commit). To install the hooks, 
run `pre-commit install` once in the root folder of this repository. Now your code will be 
automatically formatted whenever you commit. 

## Running in development
You need to start several backend services in order to be able to develop in the Armadillo.
You can choose the services by selecting docker profiles when running the docker-compose file.

### Storage
For local storage, you don't need to do anything. Data is automatically stored in the `data/` folder 
in this repository. You can choose another location in `application.yml` by changing the `storage.root-dir`
setting.

If you want to use MinIO as storage (including the test data), do the following:

1. Start the container with `docker-compose --profile minio up`
2. In your browser, go to `http://localhost:9090`
3. Log in with _molgenis_ / _molgenis_
4. Add a bucket `shared-lifecyle`
5. Copy the folders in `data/shared-lifecycle` in this repository to the bucket
6. In `application.yml`, uncomment the `minio` section.
7. Now Armadillo will automatically connect to MinIO at startup.

> **_Note_**: When you run Armadillo locally for the first time, the `lifecycle` project has not been
> added to the system metadata yet. To add it automatically, see [Application properties](#application-properties).
> Or you can add it manually:
> - Go to the Swagger UI (`http://localhost:8080/swagger-ui/index.html`)
> - Go to the `PUT /admin/projects` endpoint 
> - Add the project `lifecycle`
> 
> Now you're all set!

### Application properties
You can configure the application in `application.yml`. During development however, it is more
convenient to override these settings in a local .yml file that you do not commit to git. Here's
how to set that up:

- Next to `application.yml`, create a file `application-local.yml` (this file is ignored by git)
- Give it the following content:
```
armadillo:
  oidc-permission-enabled: false
  docker-management-enabled: true
  oidc-admin-user: <your OIDC email>

spring:
  security:
    oauth2:
      client:
        registration:
          molgenis:
            client-id: <OIDC client ID>
            client-secret: <OIDC client secret>
```

>Note: If can't configure an oauth2 client for any reason, just remove the `spring` section.

- Now, in the Run Configuration for the DatashieldServiceApplication, add the following program argument: 

```--spring.config.additional-location=file:armadillo/src/main/resources/application-local.yml```

Now the lifecycle test project (including its data) will work out of the box, and you will be able to log in 
with your OIDC account immediately.

### DataSHIELD Profiles
There are several DataSHIELD profiles you can start. At this stage these are the following:
- default: `docker-compose up -d` 
- exposome: `docker-compose --profile exposome up -d`
- omics: `docker-compose --profile omics up -d`

Besides this you need to specify the profiles in the Intellij run configuration.

Always use profile `development` in combination with these profiles:
- `development-exposome`
- `development-omics`

For example: `development, development-omics`
