# Quick Start 
MOLGENIS Armadillo facilitates federated analysis using <a href="https://datashield.org/" target="_blank">DataSHIELD</a>. 
To learn more about DataSHIELD, visit their website or our <a href="/pages/basic_concepts/" >Basic Concepts page</a>.

First we need to determine what kind of user you are:

1. :material-file-table: [Datamanager](#data-manager)

2. :material-server: [System Operator](#system-operator)

3. :material-layers-search: [Researcher](#researcher)

4. :fontawesome-solid-laptop-code: [Developer](#developer)

## Data manager
TODO
## System Operator
TODO

## Researcher
If you're doing research with Armadillo, you will need to have access to the Armadillo instance of the cohort that hosts
the data you want to work with. This guide assumes you have access to the data and that you have the URL of the
armadillo instance. 

Research can be done using R. The R package [DSMolgenisArmadillo](https://molgenis.github.io/molgenis-r-datashield/)
is used connect to data hosted using MOLGENIS Armadillo. The cohort that hosts the Armadillo instance will provide you
with a URL. Simply install the package using:
```R
install.packages("DSMolgenisArmadillo")
```
To be able to do basic research with DataSHIELD, we also need `DsBaseClient`: the DataSHIELD R package that provides
basic functionality required for research with DataSHIELD. You can install it using:
```R
install.packages('dsBaseClient', repos=c(getOption('repos'), 'http://cran.obiba.org'), dependencies=TRUE)
```
Now you can load the libraries:
```R
library(dsBaseClient)
library(DSMolgenisArmadillo)
```

With these libraries, you can now login to armadillo and see the data that is available to you:
```R
url <- "https://armadillo-demo.molgenis.net/"
token <- armadillo.get_token(url)
builder <- DSI::newDSLoginBuilder()

builder$append(
  server = "armadillo",
  url = url,
  token = token,
  driver = "ArmadilloDriver",
  profile = "xenon")
  
logindata <- builder$build()
conns <- DSI::datashield.login(logins = logindata)
```
TODO: find out how I list tables

This example assumes you're using our demo server as armadillo URL, but of course this can be any armadillo server.

If all of this succeeds, your access to Armadillo is setup correctly.

## Developer
TODO


