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
options(progress_enabled = FALSE)  # Disable DSI progress bars
options(cli.progress_enabled = TRUE)  # Keep cli progress bars enabled

# Shared environment for all test state and config
release_env <- new.env(parent = emptyenv())

# Tell DSI to search release_env for connections (instead of .GlobalEnv)
options(datashield.env = release_env)

# Load common functions
cli_progress_step("Loading helper functions")
source("lib/common-functions.R")
source("lib/upload-resource.R")
source("lib/create-resource.R")
cli_progress_done()

source("testthat/tests/helper-config.R")
configure_test()

# Set up authentication
cat("\n")
cli_h2("Authentication")
source("lib/set-admin-mode.R")
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
source("lib/setup-profiles.R")
source("lib/release-test-info.R")
cli_progress_step("Fetching available profiles")
profiles <- get_from_api_with_header("profiles", release_env$token, release_env$auth_type, release_env$armadillo_url, release_env$user)
cli_progress_done()
cli_alert_info(sprintf("Available: %s", paste(profiles$available, collapse = ", ")))
release_env$available_profiles <- profiles$available

# Download and prepare test data
cli_h2("Preparing test data")
source("lib/download-tables.R")
download_tables()
cat("\n")

# Define resource references for exposome and omics tests
release_env$exposome_ref <- tribble(
  ~file_name, ~path, ~url, ~object_name, ~format,
  "exposures.csv", file.path(release_env$test_file_path, "exposures.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/exposures.csv", "exposures", "csv",
  "description.csv", file.path(release_env$test_file_path, "description.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/description.csv", "description", "csv",
  "phenotypes.csv", file.path(release_env$test_file_path, "phenotypes.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/phenotypes.csv", "phenotypes", "csv",
  "exposomeSet.RData", file.path(release_env$test_file_path, "exposomeSet.RData"), "https://github.com/isglobal-brge/brge_data_large/raw/master/data/exposomeSet.Rdata", "exposomeSet", "RData",
)

release_env$omics_ref <- tribble(
  ~file_name, ~path, ~url, ~object_name, ~format,
  "chr1.gds", file.path(release_env$test_file_path, "chr1.gds"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr1_maf_filtered_small.vcf.gz?raw=true", "chr1", "VCF2GDS",
  "chr2.gds", file.path(release_env$test_file_path, "chr2.gds"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr2_maf_filtered_small.vcf.gz?raw=true", "chr2", "VCF2GDS",
  "ega_phenotypes.tsv", file.path(release_env$test_file_path, "ega_phenotypes.tsv"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/ega_synthetic_data_phenotypes_treated_with_nas.tsv?raw=true", "ega_phenotypes", "tsv"
)

# Build combined list of test resources to download
resources_ref <- tribble(
  ~file_name, ~path, ~url,
  basename(release_env$rda_dir), release_env$rda_dir, release_env$rda_url
)
if (!should_skip_test("ds-exposome")) {
  resources_ref <- bind_rows(resources_ref, release_env$exposome_ref[, c("file_name", "path", "url")])
}
if (!should_skip_test("ds-omics")) {
  resources_ref <- bind_rows(resources_ref, release_env$omics_ref[, c("file_name", "path", "url")])
}

# Download all test resources
download_many_sources(ref = resources_ref)
