# Basic Usage

Depending on your [role](../quick_start.md#quick-start) you have different options to interact with MOLGENIS Armadillo.

=== ":material-file-table: Data Manager"

    A [data manager](../quick_start.md#data-manager) will typically interact with Armadillo in multiple ways:
    
    - Using the [Armadillo User Interface (UI)](../basic_usage/armadillo_ui.md)
    - Using a R client [MolgenisArmadillo](../basic_usage/molgenis_armadillo.md)
    - Using [dsUpload](../basic_usage/dsupload_dsdictionaries.md) (R) to upload data with the help of predefined harmonised data dictionaries ([ds-dictionaries](../basic_usage/dsupload_dsdictionaries.md)).

=== ":material-server: System Operator"
    [System operator](../quick_start.md#system-operator): [Authentication server](../basic_usage/auth.md)

=== ":material-layers-search: Researcher"

    A [researcher](../quick_start.md#researcher) will connect to a Armadillo server with a R client [DSMolgenisArmadillo](../basic_usage/ds_molgenis_armadillo.md), this client allows for execution of DataSHIELD functions. Depending on the way your cohort has been setup you might use the R client through the [Central Analysis server (CAS)](../basic_usage/central_analysis_server.md). In both cases the researcher has installed R and Rstudio to edit and run their analysis.

=== ":fontawesome-solid-laptop-code: Developer"

    A [developer](../quick_start.md#developer) can use DataSHIELD Lite ([DSLite](../basic_usage/dslite.md)) as a serverless [DataSHIELD Interface (DSI)](https://datashield.github.io/DSI/) implementation which purpose is to mimic the behavior of a distant (virtualized or barebone) data repository server like Armadillo or Opal.
