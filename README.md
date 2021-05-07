# Armadillo service
[![Build Status](https://jenkins.dev.molgenis.org/buildStatus/icon?job=molgenis%2Fmolgenis-service-armadillo%2Fmaster)](https://jenkins.dev.molgenis.org/job/molgenis/job/molgenis-service-armadillo/job/master/)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Aarmadillo-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.molgenis%3Aarmadillo-service)

# What is the Armadillo?
The Armadillo suite can be used by data stewards to share datasets on a server. Researchers can then analyse these datasets and datasets shared on other servers using the DataSHIELD analysis tools. Researchers will only be able to access aggregate information and cannot see individual rows.

## Analyse data
The Armadillo uses the DataSHIELD platform to facilitate analysis. It contains a variety of statistical packages applicable to different research areas. There are DataSHIELD packages for standard statistical analysis, exposome studies, survival studies, microbiome studies and analysis tools for studies that are using large genetic datasets. These packages can all be installed in the Armadillo suite.

How does it work? A researcher connects from an R client to one or multiple Armadillo servers. The data is loaded into an R session on the Armadillo server specifically created for the researcher. Analysis requests are sent to the R session on each Armadillo server. There the analysis is performed and aggregated results are sent back to the client.

![DataSHIELD overview](https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/doc/img/overview-datashield.png)

## Share data
Data stewards can use the MolgenisArmadillo R client to manage their data on the Armadillo file server. Data is stored in a format that supports fast selections of the columns (variables) you need for analysis. Data stewards can manage the uploaded data in the web browser. 
The data can be stored encrypted on the Armadillo file server. 

## Access data
Everybody logs in via single sign on using a central authentication server that federates to all of the institutions. This allows people to use one set of credentials in a network of Armadillo servers.
Data stewards can grant access to dataset folders using a web application. They can find the participating users in the central authentication server.

## Getting started
The Armadillo R packages are hosted on CRAN.

To spin up your own server on a laptop, you can run `docker-compose up` in the docker folder.
* Armadillo service
* R Server
* File server

Alternatively for kubernetes deployments look at the [helm chart](https://github.com/molgenis/molgenis-ops-helm/tree/master/charts/molgenis-armadillo). 
Or for virtual server deployment look at the [ansible playbook](https://galaxy.ansible.com/molgenis/armadillo).
