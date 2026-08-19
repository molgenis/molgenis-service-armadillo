# Debug setup script - run this to set up the environment for manual testing
# Usage: R -e "source('debug-setup.R')" or from RStudio: source("debug-setup.R")
#
# After running, you'll have release_env set up with:
#   - release_env$conns (DataSHIELD connection)
#   - release_env$project1 (test project name)
#   - All other config from normal test runs
#
# You can then run individual test code manually, e.g.:
#   dsSurvivalClient::ds.Surv(time = "survival$time", event = "survival$status",
#                             objectname = "surv_object", datasources = release_env$conns)

library(cli)

# Run the standard setup (loads libraries, configures test, logs in as data manager)
cli_h1("Debug Setup")
source("lib/setup.R")

# Set current profile (normally done by release-test.R)
release_env$current_profile <- release_env$profile
setup_profiles()

# --- Core setup steps (extracted from tests 03, 04, 07, 08) ---

cli_h2("Creating test environment")

# From test-03: Create project
cli_progress_step("Creating test project")
tryCatch({
  armadillo.create_project(release_env$project1)
  cli_progress_done()
}, error = function(e) {
  if (grepl("already exists", e$message)) {
    cli_progress_done()
    cli_alert_info("Project already exists, continuing")
  } else {
    cli_progress_done(result = "failed")
    stop(e)
  }
})

# From test-04: Upload data
cli_progress_step("Uploading test data")
library(dplyr)

dest <- release_env$dest
if (dir.exists(release_env$default_parquet_path)) {
  dest <- release_env$default_parquet_path
}

nonrep <- read_parquet_with_message("core/nonrep", dest)
yearlyrep <- read_parquet_with_message("core/yearlyrep", dest)
monthlyrep <- read_parquet_with_message("core/monthlyrep", dest)
trimesterrep <- read_parquet_with_message("core/trimesterrep", dest)

armadillo.upload_table(release_env$project1, "2_1-core-1_0", nonrep)
armadillo.upload_table(release_env$project1, "2_1-core-1_0", yearlyrep)
armadillo.upload_table(release_env$project1, "2_1-core-1_0", monthlyrep)
armadillo.upload_table(release_env$project1, "2_1-core-1_0", trimesterrep)
rm(nonrep, yearlyrep, monthlyrep, trimesterrep)

nonrep <- read_parquet_with_message("outcome/nonrep", dest)
yearlyrep <- read_parquet_with_message("outcome/yearlyrep", dest)
armadillo.upload_table(release_env$project1, "1_1-outcome-1_0", nonrep)
armadillo.upload_table(release_env$project1, "1_1-outcome-1_0", yearlyrep)

veteran <- read_parquet_with_message("survival/veteran", dest)
armadillo.upload_table(release_env$project1, "survival", veteran)
rm(veteran)

armadillo.upload_table(release_env$project1, "tidyverse", mtcars)
mtcars_group <- mtcars %>% group_by(cyl)
armadillo.upload_table(release_env$project1, "tidyverse", mtcars_group)

cli_progress_done()

# From test-07: Set researcher access
# Skipped in debug mode - admin mode has full access, and non-admin mode
# requires interactive steps. If needed, run test-07 manually first.

# From test-08: Researcher login (creates release_env$conns)
cli_progress_step("Logging in as researcher")

create_dsi_builder <- function(server = "armadillo", table = "", resource = "") {
  builder <- DSI::newDSLoginBuilder()
  if (release_env$ADMIN_MODE) {
    builder$append(
      server = server, url = release_env$armadillo_url,
      profile = release_env$current_profile, table = table,
      driver = "ArmadilloDriver", user = "admin",
      password = release_env$admin_pwd, resource = resource
    )
  } else {
    builder$append(
      server = server, url = release_env$armadillo_url,
      profile = release_env$current_profile, table = table,
      driver = "ArmadilloDriver", token = release_env$token,
      resource = resource
    )
  }
  return(builder$build())
}

table <- "2_1-core-1_0/nonrep"
object <- "nonrep"
variables <- "coh_country"
full_table <- sprintf("%s/%s", release_env$project1, table)
logindata <- create_dsi_builder(table = full_table)
release_env$conns <- datashield.login(logins = logindata, symbol = object, variables = variables, assign = TRUE)
cli_progress_done()

# --- Optional: Set up specific test data ---
# Uncomment sections below as needed

# Survival data (for test-14)
# cli_progress_step("Setting up survival data")
# library(dsSurvivalClient)
# data_path <- "/survival/veteran"
# datashield.assign.table(release_env$conns, "survival", sprintf("%s%s", release_env$project1, data_path))
# cli_progress_done()

cli_h2("Ready for debugging")
cli_alert_success("Environment ready. You can now run test code manually.")
cli_alert_info(sprintf("Project: %s", release_env$project1))
cli_alert_info("Connection available as: release_env$conns")
cat("\n")
