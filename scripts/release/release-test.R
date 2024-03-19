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
# for logging nicely and showing loading spinner
library(cli)
cli_h1("Setup")
cli_alert_info("Loading libraries")
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
library(DSI)
library(dsBaseClient)
library(DSMolgenisArmadillo)
library(resourcer)

cli_alert_info("Loading common functions")
source("lib/common-functions.R")
cli_alert_success("Functions loaded")

cli_h2("Configuring test options")
source("test-cases/test-config.R")
test_config <- configure_test()
cli_alert_success("Options configured")

cli_h2("Preparing tables for tests")
source("test-cases/download-tables.R")
download_tables(dest = test_config$dest, service_location = test_config$service_location, skip_tests = test_config$skip_tests, default_parquet_path = test_config$default_parquet_path)

cli_h2("Preparing resource for tests")
source("test-cases/download-resources.R")
prepare_resources(rda_dir = test_config$rda_dir, skip_tests = test_config$skip_tests)

cli_h2("Determining whether to run with password or token")
source("test-cases/set-admin-mode.R")
token <- set_admin_or_get_token(admin_pwd = test_config$admin_pwd, url = test_config$armadillo_url, skip_tests = test_config$skip_test, ADMIN_MODE = test_config$ADMIN_MODE)

cli_h2("Configuring profiles")
source("test-cases/setup-profiles.R")
profile_info <- setup_profiles(auth_type = test_config$auth_type, token = token, skip_tests = test_config$skip_tests, url = test_config$armadillo_url, as_docker_container = test_config$as_docker_container, profile = test_config$profile, user = test_config$user, interactive = test_config$interactive, profile_defaults = test_config$profile_defaults)

cli_h1("Starting release test")
source("lib/release-test-info.R")
test_message <- show_test_info(version = test_config$version, url = test_config$armadillo_url, user = test_config$user, admin_pwd = test_config$admin_pwd, dest = test_config$dest, profile = test_config$profile, ADMIN_MODE = test_config$ADMIN_MODE, skip_tests = test_config$skip_tests)

cli_h2("Logging in as data manager")
source("test-cases/dm-login.R")
dm_login(url = test_config$armadillo_url, ADMIN_MODE = test_config$ADMIN_MODE, admin_pwd = test_config$admin_pwd, skip_tests = test_config$skip_tests)

cli_h2("Generating a random project name")
project1 <- generate_random_project_name(skip_tests = test_config$skip_tests)

cli_h2("Creating a test project")
source("test-cases/create-test-project.R")
create_test_project(target_project_name = project1, skip_tests = test_config$skip_tests)

cli_h2("Uploading test data")
source("test-cases/upload-data.R")
upload_test_data(project = project1, dest = test_config$default_parquet_path, skip_tests = test_config$skip_tests)

cli_h2("Uploading resource source file")
source("test-cases/upload-resource.R")
upload_resource(project = project1, rda_dir = test_config$rda_dir, url = test_config$armadillo_url, token = token, auth_type = test_config$auth_type, skip_tests = test_config$skip_tests)

cli_h2("Creating resource")
source("test-cases/create-resource.R")
resGSE1 <- create_resource(target_project = "u4mdd7wtwp", url = test_config$armadillo_url, skip_tests = test_config$skip_tests)

cli_h2("Uploading resource file")
armadillo.upload_resource(project = project1, folder = "ewas", resource = resGSE1, name = "GSE66351_1")

cli_h2("Starting manual UI test")
source("test-cases/manual-test.R")
interactive_test(project1, test_config$interactive, test_config$skip_tests)

cli_alert_info("\nNow you're going to test as researcher")
cli_h2("Setting researcher permissions")
source("test-cases/set_researcher_access.R")
set_researcher_access(url = test_config$armadillo_url, interactive = test_config$interactive, required_projects = list(project1), user = test_config$user, admin_pwd = test_config$admin_pwd, update_auto = test_config$update_auto, skip_tests = test_config$skip_tests) # Add linked table when working

cli_h2("Logging in as a researcher")
source("test-cases/researcher-login.R")
conns <- researcher_login(url = test_config$armadillo_url, profile = test_config$profile, admin_pwd = test_config$admin_pwd, token = token, table = "2_1-core-1_0/nonrep", project = project1, object = "nonrep", variables = "coh_country", ADMIN_MODE = test_config$ADMIN_MODE, skip_tests = test_config$skip_tests)

cli_h2("Verifying connecting to profiles possible")
source("test-cases/verify-profile.R")
verify_profiles(admin_pwd = test_config$admin_pwd, token = token, url = test_config$armadillo_url, profile = test_config$profile, ADMIN_MODE = test_config$ADMIN_MODE, skip_tests = test_config$skip_tests)

cli_h2("Assigning tables as researcher")
source("test-cases/assigning.R")
check_assigning(project = project1, folder = "2_1-core-1_0", table = "nonrep", object = "nonrep", variable = "coh_country", skip_tests = test_config$skip_tests)

cli_h2("Testing resources as a researcher")
source("test-cases/verify-resources.R")
verify_resources(project = project1, resource_path = "ewas/GSE66351_1", ADMIN_MODE = test_config$ADMIN_MODE, profile_info = profile_info, skip_tests = test_config$skip_tests)

cli_h2("Verifying xenon packages")
cli_alert_info("Verifying dsBase")
source("test-cases/ds-base.R")
verify_ds_base(object = "nonrep", variable = "coh_country", skip_tests = test_config$skip_tests)

cli_alert_info("Verifying dsMediation")
source("test-cases/xenon-mediate.R")
verify_ds_mediation(skip_tests = test_config$skip_tests)

cli_alert_info("Testing dsSurvival")
source("test-cases/xenon-survival.R")
run_survival_tests(project = project1, data_path = "/survival/veteran", skip_tests = test_config$skip_tests)

cli_alert_info("Testing dsMTL")
source("test-cases/xenon-mtl.R")
verify_ds_mtl(skip_tests = test_config$skip_tests)

cli_h2("Removing data as admin")
source("test-cases/remove-data.R") # Add link_project once module works
dm_clean_up(user = test_config$user, admin_pwd = test_config$admin_pwd, required_projects = list(project1), update_auto = test_config$update_auto, url = test_config$armadillo_url, skip_tests = test_config$skip_tests, interactive = test_config$interactive)
datashield.logout(conns)

cli_h2("Testing basic authentification")
source("test-cases/basic-auth.R")
print(test_config$dest)
verify_basic_auth(url = test_config$armadillo_url, admin_pwd = test_config$admin_pwd, dest = test_config$default_parquet_path, skip_tests = test_config$skip_tests)

cli_alert_info("Testing done")
cli_alert_info("Please test rest of UI manually, if impacted this release")
