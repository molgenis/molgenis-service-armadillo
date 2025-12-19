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

cli_alert_info("Loading common functions")
source("../../lib/common-functions.R")
cli_alert_success("Functions loaded")

cli_h2("Configuring test options")
source("../../test-cases/test-config.R")
configure_test()

cli_alert_success("Options configured")

cli_h2("Preparing tables for tests")
source("../../test-cases/download-tables.R")
download_tables(
  dest                 = getOption("ds.test.dest"),
  service_location     = getOption("ds.test.service_location"),
  skip_tests           = getOption("ds.test.skip_tests"),
  default_parquet_path = getOption("ds.test.default_parquet_path")
)

cli_h2("Preparing resource for tests")
source("../../test-cases/download-resources.R")
prepare_resources(
  resource_path = getOption("ds.test.rda_dir"),
  url           = getOption("ds.test.rda_url"),
  skip_tests     = getOption("ds.test.skip_tests")
)