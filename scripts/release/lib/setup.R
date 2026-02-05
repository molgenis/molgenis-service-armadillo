# Setup script for release tests
# This file handles all initialization and preparation before tests run

# Load cli first for logging
library(cli)
cli_h1("Setup")

# Load all required libraries
cli_alert_info("Loading libraries")
suppressPackageStartupMessages(source("lib/load-libraries.R"))
cli_alert_success("Libraries loaded")

# Configure DataSHIELD options
options(datashield.errors.print = TRUE)

# Shared environment for all test state and config
release_env <- new.env(parent = emptyenv())
# Tell DSI to search release_env for connections (instead of .GlobalEnv)
options(datashield.env = release_env)

# Load common functions
cli_alert_info("Loading common functions")
source("lib/common-functions.R")
cli_alert_success("Functions loaded")

# Configure test options
cli_h2("Configuring test options")
source("testthat/tests/helper-config.R")
configure_test()
cli_alert_success("Options configured")

# Download and prepare test data
cli_h2("Preparing tables for tests")
source("test-cases/download-tables.R")
download_tables()

cli_h2("Preparing resource for tests")
source("test-cases/download-resources.R")
prepare_resources()
