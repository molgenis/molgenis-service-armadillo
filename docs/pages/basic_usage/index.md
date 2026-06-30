# Basic Usage

Depending on your [role](../quick_start.md#quick-start) you have different options to interact with MOLGENIS Armadillo.

=== ":material-file-table: Data Manager"

    A [data manager](../quick_start.md#data-manager) will typically interact with Armadillo in multiple ways:
    
    - Using the [Armadillo User Interface (UI)](../basic_usage/armadillo_ui.md)
    - Using a R client [MolgenisArmadillo](../basic_usage/molgenis_armadillo.md)
    - Using [dsUpload](../basic_usage/dsupload_dsdictionaries.md) (R) to upload data with the help of predefined harmonised data dictionaries ([ds-dictionaries](../basic_usage/dsupload_dsdictionaries.md)).

    ### Control
    <div style="border: 1px solid #005EC4; border-radius: 3px; width: 6em; padding-top: 3px;">
        <span style="background-color:#D4ECFF; padding: 5px 0px 2.5px 5px; border-radius: 3px 0px 0px 3px; ">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="#353535" class="bi bi-tag" viewBox="0 0 16 16">
                <path d="M6 4.5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0m-1 0a.5.5 0 1 0-1 0 .5.5 0 0 0 1 0"/>
                <path d="M2 1h4.586a1 1 0 0 1 .707.293l7 7a1 1 0 0 1 0 1.414l-4.586 4.586a1 1 0 0 1-1.414 0l-7-7A1 1 0 0 1 1 6.586V2a1 1 0 0 1 1-1m0 5.586 7 7L13.586 9l-7-7H2z"/>
            </svg>
        </span>
        <span style="margin: 0 0 0.5em 0; border-left: 3px solid #017FFD; padding: 5px 0 3px 0.5em">5.14.0</span>
    </div>

    To ease some of the system administrators responsibilites, datamanagers can access the Armadillo Control page in the User Interface where they can:    

    - Configure an [Authentication server](../basic_usage/auth.md)
    - [Update/restart Armadillo](../basic_usage/application_update_restart.md#update)

=== ":material-server: System Operator"
    <div style="border: 1px solid #005EC4; border-radius: 3px; width: 6em; padding-top: 3px;">
        <span style="background-color:#D4ECFF; padding: 5px 0px 2.5px 5px; border-radius: 3px 0px 0px 3px; ">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="#353535" class="bi bi-tag" viewBox="0 0 16 16">
                <path d="M6 4.5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0m-1 0a.5.5 0 1 0-1 0 .5.5 0 0 0 1 0"/>
                <path d="M2 1h4.586a1 1 0 0 1 .707.293l7 7a1 1 0 0 1 0 1.414l-4.586 4.586a1 1 0 0 1-1.414 0l-7-7A1 1 0 0 1 1 6.586V2a1 1 0 0 1 1-1m0 5.586 7 7L13.586 9l-7-7H2z"/>
            </svg>
        </span>
        <span style="margin: 0 0 0.5em 0; border-left: 3px solid #017FFD; padding: 5px 0 3px 0.5em">5.14.0</span>
    </div>

    A [System operator](../quick_start.md#system-operator) configures the armadillo server. This person makes sure everything is running correctly. They can:

    - Configure an [Authentication server](../basic_usage/auth.md)
    - [Update/restart Armadillo](application_update_restart.md#update)

=== ":material-layers-search: Researcher"

    A [researcher](../quick_start.md#researcher) will connect to a Armadillo server with a R client [DSMolgenisArmadillo](../basic_usage/ds_molgenis_armadillo.md), this client allows for execution of DataSHIELD functions. Depending on the way your cohort has been setup you might use the R client through the [Central Analysis server (CAS)](../basic_usage/central_analysis_server.md). In both cases the researcher has installed R and Rstudio to edit and run their analysis.

=== ":fontawesome-solid-laptop-code: Developer"

    A [developer](../quick_start.md#developer) can use DataSHIELD Lite ([DSLite](../basic_usage/dslite.md)) as a serverless [DataSHIELD Interface (DSI)](https://datashield.github.io/DSI/) implementation which purpose is to mimic the behavior of a distant (virtualized or barebone) data repository server like Armadillo or Opal.
