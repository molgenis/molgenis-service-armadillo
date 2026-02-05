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
library(testthat)

options(datashield.errors.print = TRUE)

# Shared environment for all test state and config
release_env <- new.env(parent = emptyenv())
# Tell DSI to search release_env for connections (instead of .GlobalEnv)
options(datashield.env = release_env)

cli_alert_info("Loading common functions")
source("lib/common-functions.R")
cli_alert_success("Functions loaded")

cli_h2("Configuring test options")
source("testthat/tests/test-config.R")
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

    testthat::test_dir(
      "testthat/tests",
      reporter = testthat::ProgressReporter$new(),
      stop_on_failure = FALSE
    )

    cli_alert_info("Please test rest of UI manually, if impacted this release")
    end_time <- Sys.time()
    print(paste0("Running tests for profile [", profile, "] took: ", end_time - start_time))
}

lapply(profiles, run_tests_for_profile)
