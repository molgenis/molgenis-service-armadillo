#!/usr/bin/env Rscript
cat("
  __  __  ____  _      _____ ______ _   _ _____  _____                                     _ _ _ _
 |  \\/  |/ __ \\| |    / ____|  ____| \\ | |_   _|/ ____|     /\\                            | (_) | |
 | \\  / | |  | | |   | |  __| |__  |  \\| | | | | (___      /  \\   _ __ _ __ ___   __ _  __| |_| | | ___
 | |\\/| | |  | | |   | | |_ |  __| | . ` | | |  \\___ \\    / /\\ \\ | '__| '_ ` _ \\ / _` |/ _` | | | |/ _ \\
 | |  | | |__| | |___| |__| | |____| |\\  |_| |_ ____) |  / ____ \\| |  | | | | | | (_| | (_| | | | | (_) |
 |_|  |_|\\____/|______\\_____|______|_| \\_|_____|_____/  /_/    \\_\\_|  |_| |_| |_|\\__,_|\\__,_|_|_|_|\\___/

  _____      _                       _            _
 |  __ \\    | |                     | |          | |
 | |__) |___| | ___  __ _ ___  ___  | |_ ___  ___| |_
 |  _  // _ \\ |/ _ \\/ _` / __|/ _ \\ | __/ _ \\/ __| __|
 | | \\ \\  __/ |  __/ (_| \\__ \\  __/ | ||  __/\\__ \\ |_
 |_|  \\_\\___|_|\\___|\\__,_|___/\\___|  \\__\\___||___/\\__|
")
print(sessionInfo())
# for logging nicely and showing loading spinner
library(cli)
cli_h1("Setup")
cli_alert_info("Loading libraries")
library(DSI)
# for password prompt
library(getPass)
# for reading parquet files
library(arrow)
# for doing api calls
library(httr)
# for loading json to put to api
library(jsonlite)
# to post resource file async to server to be able to show spinner while loading
library(future)
# to test if url exists
library(RCurl)
# to generate random project names
library(stringi)
# armadillo/datashield libraries needed for testing
library(MolgenisArmadillo)
library(dsBaseClient)
library(DSMolgenisArmadillo)
library(resourcer)

options(datashield.errors.print = TRUE)

# Shared environment for all test state and config
release_env <- new.env(parent = emptyenv())
# Tell DSI to search release_env for connections (instead of .GlobalEnv)
options(datashield.env = release_env)

cli_alert_info("Loading common functions")
source("lib/common-functions.R")
cli_alert_success("Functions loaded")

cli_h2("Configuring test options")
source("test-cases/test-config.R")
configure_test()
cli_alert_success("Options configured")

cli_h2("Preparing tables for tests")
source("test-cases/download-tables.R")
download_tables()

cli_h2("Preparing resource for tests")
source("test-cases/download-resources.R")
prepare_resources()

profiles <- unlist(stri_split_fixed(release_env$profile, ","))


run_tests_for_profile <- function(profile) {
    start_time <- Sys.time()
    release_env$current_profile <- profile
    cli_h2(paste0("Running for profile: ", profile))

    cli_h2("Determining whether to run with password or token")
    source("test-cases/set-admin-mode.R")
    set_admin_or_get_token()

    cli_h2("Configuring profiles")
    source("test-cases/setup-profiles.R")
    setup_profiles()

    cli_h1("Starting release test")
    source("lib/release-test-info.R")
    show_test_info()

    cli_h2("Logging in as data manager")
    source("test-cases/dm-login.R")
    dm_login()

    cli_h2("Generating a random project name")
    release_env$project1 <- generate_random_project_name()

    cli_h2("Creating a test project")
    source("test-cases/create-test-project.R")
    create_test_project()

    cli_h2("Uploading test data")
    source("test-cases/upload-data.R")
    upload_test_data()

    cli_h2("Uploading resource source file")
    source("test-cases/upload-resource.R")
    upload_resource(folder = "ewas", file_name = "gse66351_1.rda")

    cli_h2("Creating resource")
    source("test-cases/create-resource.R")
    resGSE1 <- create_resource(folder = "ewas", file_name = "gse66351_1.rda", resource_name = "GSE66351_1", format = "ExpressionSet")

    cli_h2("Uploading resource file")
    armadillo.upload_resource(project = release_env$project1, folder = "ewas", resource = resGSE1, name = "GSE66351_1")

    cli_h2("Starting manual UI test")
    source("test-cases/manual-test.R")
    interactive_test()

    cli_alert_info("\nNow you're going to test as researcher")
    cli_h2("Setting researcher permissions")
    source("test-cases/set_researcher_access.R")
    set_researcher_access()

    cli_h2("Logging in as a researcher")
    source("test-cases/researcher-login.R")
    researcher_login(table = "2_1-core-1_0/nonrep", object = "nonrep", variables = "coh_country")

    cli_h2("Verifying connecting to profiles possible")
    source("test-cases/verify-profile.R")
    verify_profiles()

    cli_h2("Assigning tables as researcher")
    source("test-cases/assigning.R")
    check_assigning(folder = "2_1-core-1_0", table = "nonrep", object = "nonrep", variable = "coh_country")

    cli_h2("Testing resources as a researcher")
    source("test-cases/verify-resources.R")
    verify_resources(resource_path = "ewas/GSE66351_1")

    cli_h2("Verifying xenon packages")
    cli_alert_info("Verifying dsBase")
    source("test-cases/ds-base.R")
    verify_ds_base(object = "nonrep", variable = "coh_country")

    cli_alert_info("Verifying dsMediation")
    source("test-cases/xenon-mediate.R")
    verify_ds_mediation()

    cli_alert_info("Testing dsSurvival")
    source("test-cases/xenon-survival.R")
    run_survival_tests(data_path = "/survival/veteran")

    cli_alert_info("Testing dsMTL")
    source("test-cases/xenon-mtl.R")
    verify_ds_mtl()

    cli_alert_info("Testing dsExposome")
    source("test-cases/xenon-exposome.R")
    run_exposome_tests()

    cli_alert_info("Testing dsOmics")
    source("test-cases/xenon-omics.R")
    run_omics_tests()

    cli_alert_info("Testing dsTidyverse")
    source("test-cases/donkey-tidyverse.R")
    run_tidyverse_tests(data_path = "/tidyverse")

    cli_h2("Removing data as admin")
    source("test-cases/remove-data.R") # Add link_project once module works
    dm_clean_up()
    datashield.logout(release_env$conns)

    cli_h2("Testing basic authentication")
    source("test-cases/basic-auth.R")
    verify_basic_auth()

    cli_alert_info("Testing done")
    cli_alert_info("Please test rest of UI manually, if impacted this release")
    end_time <- Sys.time()
    print(paste0("Running tests for profile [", profile, "] took: ", end_time - start_time))
}

lapply(profiles, run_tests_for_profile)
