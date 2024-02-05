# Armadillo suite

[![CircleCI](https://circleci.com/gh/molgenis/molgenis-service-armadillo.svg?style=shield)](https://circleci.com/gh/molgenis/molgenis-service-armadillo)
[![Build Status](https://dev.azure.com/molgenis/molgenis-service-armadillo/_apis/build/status/molgenis.molgenis-service-armadillo?branchName=master)](https://dev.azure.com/molgenis/molgenis-service-armadillo/_build/latest?definitionId=1&branchName=master)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Aarmadillo-service&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=org.molgenis%3Aarmadillo-service)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=org.molgenis%3Aarmadillo-service&metric=alert_status)](https://sonarcloud.io/dashboard?id=org.molgenis%3Aarmadillo-service)

# Overview

Use MOLGENIS/Armadillo to make data available for privacy protecting federated analysis using [DataSHIELD](https://datashield.org) protocol. Armadillo
service provides the following features:
* **manage data projects**. Projects can either hold tabular data in the efficient 'parquet' format or any other file use DataSHIELD
  'resources' framework.
* **grant users access permission**. We use a central OIDC service like KeyCloak or FusionAuth in combination with a trused identity provider like
  Life Sciences AAI to authenticate users.
* **configure DataSHIELD analysis profiles**. [DataSHIELD analysis profiles](https://www.datashield.org/help/standard-profiles-and-plaforms) are
  Docker images that contain a collection of multiple [DataSHIELD analysis packages](https://www.datashield.org/help/community-packages).

![DataSHIELD overview](https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/master/docs/img/overview-datashield.png)

## Getting started

For installing and using Armadillo see https://molgenis.github.io/molgenis-service-armadillo/#/

For developing and contributing see [Contributing](./CONTRIBUTING.md).
 
