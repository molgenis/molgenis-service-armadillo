# Basic concepts
DataSHIELD is a software solution that allows for secure data science collaboration. It's designed for remote, 
non-disclosive analysis of sensitive data and is very suitable for federated analysis. In order to use DataSHIELD, 
additional software is required to store data and manage user interaction. MOLGENIS Armadillo is designed to do this.

This all might seem a bit abstract. The best way to explain how this all works, is by visualising it. Below you see an
example of a simple setup for using DataSHIELD in production.

![Simple setup](../img/ds-simple-setup.png){ width="600" }
/// caption
A user writes analysis commands in R, using client side DataSHIELD R packages. The commands are sent to the biobank 
servers. In Armadillo's case, the R package [DSMolgenisArmadillo](https://molgenis.github.io/molgenis-r-datashield/) is 
used for this. The servers run DataSHIELD serverside R packages using either Armadillo or Opal. The non-disclosive
statistical response to the commands will be returned to the user.  
///

Please note that users can use both Opal and Armadillo servers in their research. 
As long as the data is harmonised the same way, it is completely compatible. 
Although the setup above is fully functional, usually we have a setup that looks more like this:

![ds-complete-setup.png](../img/ds-complete-setup.png)
/// caption
A user logs in to the Central Analysis Server (CAS), which is basically an online R studio research environment. On
there users can write their analysis code. Biobank servers are blocked off from the rest of the internet by a firewall
and can only be accessed from the CAS. This is an additional layer of protection to ensure the biobank data is safely
and securely stored on their servers. In this setup, users don't have to install R packages by themselves, another added
benefit.
///

Another thing to note in the image is the possibility to host files elsewhere (e.g. computer clusters) and link them to
the data of armadillo or opal servers. Of course this feature is optional, as well as flexible. External resources can
be used alongside data stored in armadillo itself and resources may be hosted in different locations and formats.

Now that we explained the basic concepts of DataSHIELD and MOLGENIS Armadillo's role, we think it's important to 
elaborate on the different software packages available and what they do.

|             **Name** | **Type of application**                                    | **User type** | **Description**                                                                                                   | Sources                                                                                                                                    |
|---------------------:|------------------------------------------------------------|---------------|-------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|
|   MOLGENIS Armadillo | :material-language-java: Java                              | -             | The software that facilitates hosting and analysing data using DataSHIELD.                                        | [:simple-github:](https://github.com/molgenis/molgenis-service-armadillo)                                                                           |
|                 - UI | :simple-javascript: Javascript/<br/>:material-vuejs: VueJS | Datamanager   | To manage users, data, configurations and view logs.                                                              | [:simple-github:](https://github.com/molgenis/molgenis-service-armadillo/tree/master/ui)                                                            |
|    MolgenisArmadillo | :simple-r: R package                                       | Datamanager   | To manage upload data and manage projects in Armadillo in R.                                                      | [:simple-github:](https://github.com/molgenis/molgenis-R-armadillo) [:simple-r:](https://cran.r-project.org/web/packages/MolgenisArmadillo/index.html)    |
|             DsUpload | :simple-r: R package                                       | Datamanager   | To upload data according to a specific format into Armadillo/Opal using R.                                        | [:simple-github:](https://github.com/lifecycle-project/ds-upload)                                                                                   |
|  DSMolgenisArmadillo | :simple-r: R package                                       | Researchers   | To connect to MOLGENIS Armadillo so that DataSHIELD R packages can be used to do analysis.                        | [:simple-github:](https://github.com/molgenis/molgenis-R-datashield) [:simple-r:](https://cran.r-project.org/web/packages/DSMolgenisArmadillo/index.html) |
|         DsBaseClient | :simple-r: R package                                       | Researchers   | Basic DataSHIELD R package required for executing basic DataSHIELD commands.                                      | [:simple-github:](https://github.com/datashield/dsBaseClient)                                                                                       |
|    DsTidyverseClient | :simple-r: R package                                       | Researchers   | The client R package that allows users to use selected tidyverse functions within their DataSHIELD analysis code. | [:simple-github:](https://github.com/molgenis/ds-tidyverse-client) [:simple-r:](https://cran.r-project.org/web/packages/dsTidyverseClient/index.html)     |
