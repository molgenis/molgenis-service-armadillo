#!/usr/bin/env Rscript
#
# testthat.R - Main test runner for Armadillo release tests
#
# This script runs the testthat-based release tests with optional filtering.
#
# Usage:
#   Rscript testthat.R                                    # Run all tests
#   Rscript testthat.R --only resources                   # Run only resources cluster
#   Rscript testthat.R --only resources data-manager      # Run resources + data-manager
#   Rscript testthat.R --skip ds-omics                    # Run all except ds-omics
#   Rscript testthat.R --skip ds-omics ds-exposome        # Skip multiple tests
#   Rscript testthat.R --only resources --skip ds-omics   # Resources without ds-omics
#   Rscript testthat.R --list                             # List available clusters/tests
#
# Named clusters:
#   all                  - All tests
#   data-manager         - Data manager and basic auth tests
#   researcher-tables    - Researcher tests for tabular data (no resources)
#   researcher-resources - Researcher tests that use resources (exposome, omics)
#
# Individual tests (can also be used with --only/--skip):
#   data-manager, researcher-login, profiles, assigning-tables, assigning-resources,
#   ds-base, ds-mediate, ds-survival, ds-mtl, ds-exposome, ds-omics, ds-tidyverse,
#   basic-auth
#
# Environment variables (can also be set in .env file):
#   TEST_ONLY - Space-separated list of clusters/tests to run (e.g., "resources data-manager")
#   TEST_SKIP - Space-separated list of clusters/tests to skip (e.g., "ds-omics ds-exposome")
#   See .env file for full configuration options

# -----------------------------------------------------------------------------
# Named test clusters
# -----------------------------------------------------------------------------

# Named test clusters (groups of tests)
TEST_CLUSTERS <- list(
  # All tests
  all = "10-data-manager|20-researcher-login|21-profiles|22-assigning-tables|23-assigning-resources|30-ds-base|31-ds-mediate|32-ds-survival|33-ds-mtl|34-ds-exposome|35-ds-omics|36-ds-tidyverse|80-basic-auth",

  # Data manager tests (includes basic-auth)
  `data-manager` = "10-data-manager|80-basic-auth",

  # Researcher tests for tabular data (no resources)
  `researcher-tables` = "20-researcher-login|21-profiles|22-assigning-tables|30-ds-base|31-ds-mediate|32-ds-survival|33-ds-mtl|36-ds-tidyverse",

  # Researcher tests that use resources (no table assignment needed)
  `researcher-resources` = "20-researcher-login|21-profiles|23-assigning-resources|34-ds-exposome|35-ds-omics"
)

# Individual test mappings (test name -> file pattern)
TEST_INDIVIDUALS <- list(
  `data-manager`        = "10-data-manager",
  `researcher-login`    = "20-researcher-login",
  profiles              = "21-profiles",
  `assigning-tables`    = "22-assigning-tables",
  `assigning-resources` = "23-assigning-resources",
  `ds-base`             = "30-ds-base",
  `ds-mediate`          = "31-ds-mediate",
  `ds-survival`         = "32-ds-survival",
  `ds-mtl`              = "33-ds-mtl",
  `ds-exposome`         = "34-ds-exposome",
  `ds-omics`            = "35-ds-omics",
  `ds-tidyverse`        = "36-ds-tidyverse",
  `basic-auth`          = "80-basic-auth"
)

# Helper function to resolve a name to its pattern(s)
resolve_test_name <- function(name) {
  if (name %in% names(TEST_CLUSTERS)) {
    return(TEST_CLUSTERS[[name]])
  } else if (name %in% names(TEST_INDIVIDUALS)) {
    return(TEST_INDIVIDUALS[[name]])
  } else {
    # Assume it's a raw pattern
    return(name)
  }
}

# Helper function to get all test patterns from a list of names
get_test_patterns <- function(names) {
  patterns <- sapply(names, resolve_test_name)
  # Split any pipe-separated patterns and flatten
  all_patterns <- unlist(strsplit(patterns, "\\|"))
  unique(all_patterns)
}

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

# Suppress package startup messages and warnings
suppressPackageStartupMessages({
  suppressWarnings({
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
  })
})

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
# Parse command line arguments and environment variables
# -----------------------------------------------------------------------------

args <- commandArgs(trailingOnly = TRUE)

# Show help if requested
if (any(args %in% c("--help", "-h", "help"))) {
  cli::cli_h2("Usage")
  cli::cli_text("  Rscript testthat.R [options]")
  cli::cli_text("")
  cli::cli_h2("Options")
  cli::cli_text("  --only <names>   Run only specified clusters/tests (space-separated)")
  cli::cli_text("  --skip <names>   Skip specified clusters/tests (space-separated)")
  cli::cli_text("  --list           List available clusters and tests")
  cli::cli_text("  --help           Show this help message")
  cli::cli_text("")
  cli::cli_h2("Examples")
  cli::cli_text("  Rscript testthat.R                                  # Run all tests")
  cli::cli_text("  Rscript testthat.R --only resources                 # Run resources cluster")
  cli::cli_text("  Rscript testthat.R --only resources data-manager    # Run multiple clusters")
  cli::cli_text("  Rscript testthat.R --skip ds-omics ds-exposome      # Skip specific tests")
  cli::cli_text("  Rscript testthat.R --only resources --skip ds-omics # Combine --only and --skip")
  quit(status = 0)
}

# Show list if requested
if (any(args %in% c("--list", "-l", "list"))) {
  cli::cli_h2("Available clusters")
  for (name in names(TEST_CLUSTERS)) {
    cli::cli_text(sprintf("  %-20s %s", name, TEST_CLUSTERS[[name]]))
  }
  cli::cli_text("")
  cli::cli_h2("Individual tests")
  for (name in names(TEST_INDIVIDUALS)) {
    cli::cli_text(sprintf("  %-20s %s", name, TEST_INDIVIDUALS[[name]]))
  }
  quit(status = 0)
}

# Parse --only and --skip arguments
parse_flag_values <- function(args, flag) {
  values <- c()
  capture <- FALSE
  for (arg in args) {
    if (arg == flag) {
      capture <- TRUE
    } else if (startsWith(arg, "--")) {
      capture <- FALSE
    } else if (capture) {
      values <- c(values, arg)
    }
  }
  values
}

only_args <- parse_flag_values(args, "--only")
skip_args <- parse_flag_values(args, "--skip")

# Also check environment variables (set in .env file)
env_only <- Sys.getenv("TEST_ONLY", "")
env_skip <- Sys.getenv("TEST_SKIP", "")

if (env_only != "") {
  env_only_values <- strsplit(trimws(env_only), "\\s+")[[1]]
  only_args <- c(only_args, env_only_values)
  cli::cli_alert_info(sprintf("TEST_ONLY from env: %s", env_only))
}

if (env_skip != "") {
  env_skip_values <- strsplit(trimws(env_skip), "\\s+")[[1]]
  skip_args <- c(skip_args, env_skip_values)
  cli::cli_alert_info(sprintf("TEST_SKIP from env: %s", env_skip))
}

# Build the filter pattern
if (length(only_args) > 0) {
  # Get all patterns from --only arguments
  only_patterns <- get_test_patterns(only_args)
  cli::cli_alert_info(sprintf("Including: %s", paste(only_args, collapse = ", ")))
} else {
  # Default: all tests
  only_patterns <- get_test_patterns("all")
}

if (length(skip_args) > 0) {
  # Remove skip patterns from the only patterns
  skip_patterns <- get_test_patterns(skip_args)
  cli::cli_alert_info(sprintf("Skipping: %s", paste(skip_args, collapse = ", ")))
  only_patterns <- setdiff(only_patterns, skip_patterns)
}

# Build the final filter pattern (OR of all remaining patterns)
filter_pattern <- if (length(only_patterns) > 0) {
  paste(only_patterns, collapse = "|")
} else {
  cli::cli_alert_warning("No tests to run after applying filters!")
  NULL
}

if (!is.null(filter_pattern)) {
  cli::cli_alert_success(sprintf("Test filter: %s", filter_pattern))
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
