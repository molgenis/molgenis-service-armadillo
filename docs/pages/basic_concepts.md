# Basic concepts
[DataSHIELD](https://datashield.org/) is an R-based software solution for federated analysis - the remote analysis of multiple data sources.
It allows for sophisticated analyses without the user being able to view or copy individual level data. Instead, only non-disclosive
summary statistics are returned. This makes it an effective solution for secure data science collaborations.

In order to use DataSHIELD, additional software is required to store data and manage user interaction. There are currently two solutions for doing this: Armadillo and [Opal](https://opaldoc.obiba.org/), which can be used compatibly within the same network. Below is an example of a simple setup:

### Example setup 1
![Simple setup](../img/ds-simple-setup.png){ width="600" }
/// caption
A user writes analysis commands in R, using client-side DataSHIELD R packages. The commands are sent to the biobank 
servers. For Armadillo, the communication between the client and the server is handled by the R packages [DSI](https://github.com/datashield/DSI) and [DSMolgenisArmadillo](https://molgenis.github.io/molgenis-r-datashield/), whilst the data storage and execution of commands on the server is handled by [ArmadilloService](https://molgenis.github.io/molgenis-service-armadillo/). The non-disclosive summary statistics are then returned to the user.  
///

### Example setup 2
An alternative setup involves the user first connecting to a Central Analysis Server (CAS), which is an online R studio environment:

![ds-complete-setup.png](../img/ds-complete-setup.png)
/// caption
Once logged in to the CAS, users write their code as if they were running RStudio locally. The advantage of this setup is that Biobank servers can be configured so that they are blocked off from the rest of the internet by a firewall and can only be accessed from the CAS. This provices an additional layer of data protection. It also benefits users, as all required DataSHIELD R packages can be pre-installed thus removing the needs for users to set up their R environment.
///

### Resources
An additional optional feature of DataSHIELD is the ability to host files elsewhere (e.g. computer clusters) and link them to
the data of armadillo or opal servers. This is impletmented using the [resourcer](https://github.com/obiba/resourcer) package. External resources can
be used alongside data stored in armadillo itself, and resources may be hosted in different locations and formats.

### DataSHIELD packages and their use
Finally, here is a brief summary of the core Armadillo and DataSHIELD packages described in this documentation.

|             **Name** | **Type of application**                                    | **User type**   | **Description**                                                                                                   | Sources                                                                                                                                    |
|---------------------:|------------------------------------------------------------|-----------------|-------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
|   armadilloService   | :material-language-java: Java                              | -               | The software that facilitates hosting and analysing data using DataSHIELD.                                        | [:simple-github:](https://github.com/molgenis/molgenis-service-armadillo)                                                                 |
|                  DSI | :simple-r: R package                                       | -               | This package defines the API that is to be implemented by DataSHIELD compliant data repositories.               | [:simple-github:](https://github.com/datashield/DSI)                                                                                      |
|  DSMolgenisArmadillo | :simple-r: R package                                       | Researchers     | To communicate between the client-side packages and armadilloService to perform analysis.                         | [:simple-github:](https://github.com/molgenis/molgenis-R-datashield) [:simple-r:](https://cran.r-project.org/web/packages/DSMolgenisArmadillo/index.html) |
|    MolgenisArmadillo | :simple-r: R package                                       | Datamanager     | To upload data and manage projects in Armadillo in R.                                                             | [:simple-github:](https://github.com/molgenis/molgenis-R-armadillo) [:simple-r:](https://cran.r-project.org/web/packages/MolgenisArmadillo/index.html)    |
|        User Interface| :simple-javascript: Javascript/<br/>:material-vuejs: VueJS | Datamanager     | To manage users, data, configurations and view logs.                                                              | [:simple-github:](https://github.com/molgenis/molgenis-service-armadillo/tree/master/ui)                                                  |
|             dsUpload | :simple-r: R package                                       | Datamanager     | To upload data according to a specific format into Armadillo/Opal using R.                                        | [:simple-github:](https://github.com/lifecycle-project/ds-upload)                                                                        |
|         dsBaseClient | :simple-r: R package                                       | Researchers     | Core DataSHIELD client-side R package required for executing basic DataSHIELD commands.                           | [:simple-github:](https://github.com/datashield/dsBaseClient)                                                                             |
|    dsTidyverseClient | :simple-r: R package                                       | Researchers     | Client-side R package which implements efficient data-manipulation using selected Tidyverse functions.            | [:simple-github:](https://github.com/molgenis/ds-tidyverse-client) [:simple-r:](https://cran.r-project.org/web/packages/dsTidyverseClient/index.html)     |


