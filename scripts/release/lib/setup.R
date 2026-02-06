# Setup script for release tests
# This file handles all initialization and preparation before tests run

# Load cli first for logging
library(cli)
options(cli.num_colors = 256)
cli_h1("Setup")

cli_h2("Loading")

# Load all required libraries
cli_progress_step("Loading libraries")
suppressWarnings(suppressPackageStartupMessages(source("lib/load-libraries.R")))
cli_progress_done()

# Configure DataSHIELD options
options(datashield.errors.print = TRUE)

# Shared environment for all test state and config
release_env <- new.env(parent = emptyenv())

# Tell DSI to search release_env for connections (instead of .GlobalEnv)
options(datashield.env = release_env)

# Load common functions
cli_progress_step("Loading helper functions")
source("lib/common-functions.R")
cli_progress_done()

source("testthat/tests/helper-config.R")
configure_test()

# Set up authentication
cat("\n")
cli_h2("Authentication")
source("test-cases/set-admin-mode.R")
set_admin_or_get_token()

# Login as data manager
cli_progress_step("Logging in as data manager")
if (release_env$ADMIN_MODE) {
  suppressMessages(armadillo.login_basic(release_env$armadillo_url, "admin", release_env$admin_pwd))
} else {
  suppressMessages(armadillo.login(release_env$armadillo_url))
}
cli_progress_done()

# Get available profiles
cat("\n")
cli_h2("Profiles")
source("test-cases/setup-profiles.R")
source("lib/release-test-info.R")
cli_progress_step("Fetching available profiles")
profiles <- get_from_api_with_header("profiles", release_env$token, release_env$auth_type, release_env$armadillo_url, release_env$user)
cli_progress_done()
cli_alert_info(sprintf("Available: %s", paste(profiles$available, collapse = ", ")))
release_env$available_profiles <- profiles$available

# Download and prepare test data
cli_h2("Preparing test data")
source("test-cases/download-tables.R")
download_tables()
source("test-cases/download-resources.R")
prepare_resources()
