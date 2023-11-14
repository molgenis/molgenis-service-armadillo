# Using Armadillo
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
