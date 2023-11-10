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

