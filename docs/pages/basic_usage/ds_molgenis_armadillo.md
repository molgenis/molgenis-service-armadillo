# DSMolgenisArmadillo

Relevant for: :material-layers-search:{title="Researchers"}

Use DSMolgenisArmadillo to analyse data shared in MOLGENIS Armadillo servers using DataSHIELD. DataSHIELD allows execution of a subset of analysis methods available in R. Methods such as:

`ds.mean()` `ds.glm()` `ds.lmerSLMA()`

For more detailed information please visit [DSMolgenisArmadillo](https://molgenis.github.io/molgenis-r-datashield/) and additionally check the documentation: [https://cran.datashield.org/](https://cran.datashield.org/).

=== "Prerequisite"

    ???+ example

        ```r linenums="1" title="Prerequisite.r"
        install.packages("DSI")
        install.packages("DSMolgenisArmadillo")
        install.packages("dsBaseClient", repos = c("http://cran.datashield.org", "https://cloud.r-project.org/"), dependencies = TRUE)

        library(dsBaseClient)
        library(DSMolgenisArmadillo)
        ```
=== "Basic usage"
    ???+ example

        ```r linenums="1" title="Analyse_data_subset_DSMolgenisArmadillo.r"
        library(dsBaseClient)
        library(DSMolgenisArmadillo)

        # specify server url
        armadillo_url <- "https://armadillo.test.molgenis.org"

        # get token from central authentication server
        token <- armadillo.get_token(armadillo_url)


        # build the login dataframe
        builder <- DSI::newDSLoginBuilder()
        builder$append(
            server = "armadillo",
            url = armadillo_url,
            token = token,
            table = "workshop1/2_1-core-1_0/nonrep",
            driver = "ArmadilloDriver"
        )

        # create loginframe
        logindata <- builder$build()

        # login into server
        conns <- datashield.login(
            logins = logindata, 
            symbol = "core_nonrep", 
            variables = c("coh_country"), 
            assign = TRUE
        )

        # calculate the mean
        ds.mean("core_nonrep$coh_country", datasources = conns)

        ds.histogram(x = "core_nonrep$coh_country", datasources = conns)
        ```

!!! info
    A researcher only has access to data if he/she has been granted access to this data (typically) by a data manager or administrator of a cohorts Armadillo server.

!!! note
    The example files used in the examples above can be found here: [nonrep.parquet](../../data/nonrep.parquet), [yearlyrep.parquet](../../data/yearlyrep.parquet)

    Load these `parquet` files in R and see which variables you can call.
