#!/usr/bin/env Rscript
#
# testthat.R - Main test runner for Armadillo release tests
#
# This script runs the testthat-based release tests with optional filtering.
#
# Usage:
#   Rscript testthat.R                    # Run all tests
#   Rscript testthat.R <cluster>          # Run a named cluster (see below)
#   Rscript testthat.R "30-ds-base"       # Run specific test file (regex)
#
# Named clusters:
#   data-manager   - Data manager setup tests
#   researcher-all - All researcher tests (ds-base + all ds-packages)
#   researcher-tab - Researcher tabular data tests (no resources)
#   researcher-res - Researcher resource tests (exposome, omics)
#   config         - Configuration validation tests
#   quick          - Fast tests only (config, downloads, basic-auth)
#   basic-auth     - Basic authentication tests only
#
# Environment variables:
#   SKIP_TESTS - Comma-separated list of tests to skip (e.g., "ds-package-omics,ds-package-mtl")
#   See .env file for full configuration options

# -----------------------------------------------------------------------------
# Named test clusters
# -----------------------------------------------------------------------------

TEST_CLUSTERS <- list(
  # Role-based clusters
  `data-manager`    = "10-admin-setup",
  `researcher-all`  = "20-researcher|30-ds-base|31-ds-package|32-ds-package|33-ds-package|34-ds-package|35-ds-package|36-ds-package",
  `researcher-tab`  = "20-researcher|30-ds-base|31-ds-package|32-ds-package|33-ds-package|36-ds-package",
  `researcher-res`  = "20-researcher|34-ds-package-exposome|35-ds-package-omics",

  # Setup and utility clusters
  config            = "01-config|02-downloads",
  `basic-auth`      = "80-basic-auth",
  quick             = "01-config|02-downloads|80-basic-auth"
)

# -----------------------------------------------------------------------------
# Enable colors and full error output
# -----------------------------------------------------------------------------

# Force ANSI colors in terminal output
Sys.setenv(CLI_NO_COLORS = "false")
options(
  cli.num_colors = 256,           # Enable 256 colors
  crayon.enabled = TRUE,          # Enable crayon colors
  testthat.default_check_reporter = "progress"
)

# Show full error messages (no truncation)
options(
  warning.length = 8170,          # Max warning message length
  width = 200,                    # Console width for wrapping
  testthat.progress.max_fails = Inf,  # Show all failures, don't truncate
  rlang_backtrace_on_error = "full"   # Full backtraces on error
)

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

  (testthat version)
")

# Print session info for debugging
print(sessionInfo())

# -----------------------------------------------------------------------------
# Load required libraries
# -----------------------------------------------------------------------------

cli::cli_h1("Setup")
cli::cli_alert_info("Loading libraries...")

# Core test framework
library(testthat)

# Logging and UI
library(cli)

# DataSHIELD libraries
library(DSI)
library(MolgenisArmadillo)
library(DSMolgenisArmadillo)
library(dsBaseClient)
library(resourcer)

# Data libraries
library(arrow)
library(dplyr)
library(purrr)
library(tibble)
library(stringr)
library(stringi)

# API/HTTP libraries
library(httr)
library(jsonlite)
library(RCurl)
library(base64enc)

# Password prompt (for interactive mode)
library(getPass)

# Async operations
library(future)

# Xenon-specific libraries (loaded conditionally in tests)
# library(dsSurvivalClient)
# library(dsMediationClient)
# library(dsMTLClient)
# library(dsExposomeClient)
# library(dsOmicsClient)
# library(dsTidyverseClient)

# Set DataSHIELD error printing
options(datashield.errors.print = TRUE)

cli::cli_alert_success("Libraries loaded")

# -----------------------------------------------------------------------------
# Load helper files
# -----------------------------------------------------------------------------

cli::cli_alert_info("Loading helper files...")

# Source the original common functions (for functions we don't port)
source("../lib/common-functions.R")

# Source our setup and helper files
source("setup.R")
source("helper-functions.R")
source("helper-expectations.R")

cli::cli_alert_success("Helper files loaded")

# -----------------------------------------------------------------------------
# Parse command line arguments
# -----------------------------------------------------------------------------

args <- commandArgs(trailingOnly = TRUE)
filter_pattern <- if (length(args) > 0) args[1] else NULL

# Check if the argument is a named cluster
if (!is.null(filter_pattern) && filter_pattern %in% names(TEST_CLUSTERS)) {
  cluster_name <- filter_pattern
  filter_pattern <- TEST_CLUSTERS[[cluster_name]]
  cli::cli_alert_info(sprintf("Running cluster '%s': %s", cluster_name, filter_pattern))
} else if (!is.null(filter_pattern)) {
  cli::cli_alert_info(sprintf("Test filter pattern: %s", filter_pattern))
}

# Show available clusters if --help is passed
if (!is.null(filter_pattern) && filter_pattern %in% c("--help", "-h", "help")) {
  cli::cli_h2("Available test clusters")
  for (name in names(TEST_CLUSTERS)) {
    cli::cli_alert_info(sprintf("  %-12s %s", name, TEST_CLUSTERS[[name]]))
  }
  cli::cli_text("")
  cli::cli_alert_info("Usage: Rscript testthat.R [cluster|pattern]")
  quit(status = 0)
}

# -----------------------------------------------------------------------------
# Show test configuration
# -----------------------------------------------------------------------------

cli::cli_h2("Test Configuration")

# Load config early to show info
ensure_config()

cli::cli_ul(c(
  sprintf("Armadillo URL: %s", test_env$config$armadillo_url),
  sprintf("Version: %s", test_env$config$version),
  sprintf("Profile: %s", test_env$config$profile),
  sprintf("Admin Mode: %s", test_env$config$ADMIN_MODE),
  sprintf("Skip Tests: %s", paste(test_env$config$skip_tests, collapse = ", "))
))

# -----------------------------------------------------------------------------
# Teardown function - ALWAYS runs, even on failure
# -----------------------------------------------------------------------------

run_teardown <- function() {
  cli::cli_h2("Teardown")

  config <- test_env$config

  # 1. Re-add admin permissions to user (if in OIDC mode)
  if (!is.null(config) && !config$ADMIN_MODE && config$update_auto == "y") {
    cli::cli_alert_info("Restoring admin permissions...")
    tryCatch({
      set_user(
        user = config$user,
        admin_pwd = config$admin_pwd,
        isAdmin = TRUE,
        required_projects = list(test_env$project),
        url = config$armadillo_url
      )
      cli::cli_alert_success("Admin permissions restored")
    }, error = function(e) {
      cli::cli_alert_warning(sprintf("Could not restore admin permissions: %s", e$message))
    })
  }

  # 2. Delete test project if it was created
  if (!is.null(test_env$project)) {
    cli::cli_alert_info(sprintf("Deleting test project [%s]...", test_env$project))
    tryCatch({
      # Need to login again if session expired
      if (!is.null(config)) {
        if (config$ADMIN_MODE) {
          MolgenisArmadillo::armadillo.login_basic(config$armadillo_url, "admin", config$admin_pwd)
        } else {
          MolgenisArmadillo::armadillo.login(config$armadillo_url)
        }
      }
      MolgenisArmadillo::armadillo.delete_project(test_env$project)
      cli::cli_alert_success(sprintf("Project [%s] deleted", test_env$project))
    }, error = function(e) {
      cli::cli_alert_warning(sprintf("Could not delete project: %s", e$message))
    })
  }

  # 3. Logout from DataSHIELD connections
  if (!is.null(test_env$conns)) {
    cli::cli_alert_info("Logging out from DataSHIELD...")
    tryCatch({
      DSI::datashield.logout(test_env$conns)
      cli::cli_alert_success("Logged out successfully")
    }, error = function(e) {
      cli::cli_alert_warning(sprintf("Logout error: %s", e$message))
    })
  }
}

# -----------------------------------------------------------------------------
# Run tests
# -----------------------------------------------------------------------------

cli::cli_h1("Running Tests")

start_time <- Sys.time()

# Run tests with guaranteed teardown
test_results <- tryCatch({
  testthat::test_dir(
    "tests",
    filter = filter_pattern,
    reporter = ProgressReporter$new(),
    stop_on_failure = FALSE
  )
}, error = function(e) {
  cli::cli_alert_danger(sprintf("Test error: %s", e$message))
  NULL
}, finally = {
  # ALWAYS run teardown, even on error
  run_teardown()
})

end_time <- Sys.time()

# -----------------------------------------------------------------------------
# Summary
# -----------------------------------------------------------------------------

cli::cli_h1("Summary")

duration <- difftime(end_time, start_time, units = "secs")
cli::cli_alert_info(sprintf("Total time: %.1f seconds", as.numeric(duration)))

cli::cli_alert_info("Testing complete")
cli::cli_alert_info("Please test rest of UI manually, if impacted this release")
