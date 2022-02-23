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

## Getting started
The Armadillo R packages are hosted on CRAN.

To spin up your own server on a laptop, you can run `docker-compose up` in the docker folder.
* Armadillo service
* [R Server](https://www.rforge.net/Rserve/)
* [File server](https://minio.io)

Alternatively for [kubernetes](https://k8s.io) deployments look at the [helm chart](https://github.com/molgenis/molgenis-ops-helm/tree/master/charts/molgenis-armadillo). 
Or for virtual server deployment look at the [ansible playbook](https://galaxy.ansible.com/molgenis/armadillo).

### Development
You need to start several backend services in order to be able to develop in the Armadillo.
You can choose the services by defining a profile when running the compose file.

There are several profiles you can start. At this stage these are the following:
- default: `docker-compose up -d` 
- exposome: `docker-compose --profile exposome up -d`
- omics: `docker-compose --profile omics up -d`

Besides this you need to specify the profiles in the Intellij run configuration.

Always use profile `development` in combination with these profiles:
- `development-exposome`
- `development-omics`

For example: `development, development-omics`