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
library(dsMediationClient)
library(dsMTLClient)

# set when admin password given + question answered with y
update_auto = ""
do_run_spinner <- TRUE
ADMIN_MODE <- FALSE

# default profile settings in case a profile is missing
profile_defaults = data.frame(
  name = c("xenon", "rock"),
  container = c("datashield/rock-dolomite-xenon:latest", "datashield/rock-base:latest"),
  port = c("", ""),
  # Multiple packages can be concatenated using ,, then using stri_split_fixed() to break them up again
  # Not adding dsBase since that is always(?) required
  whitelist = c("resourcer,dsMediation,dsMTLBase", ""),
  blacklist = c("", "")
)

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
setup_profiles(auth_type = test_config$auth_type, token = token, skip_tests = test_config$skip_tests, url = test_config$armadillo_url, as_docker_container = test_config$as_docker_container, profile = test_config$profile)

cli_h1("Starting release test")
source("test-cases/release-test-info.R")
test_message <- show_test_info(version = test_config$version, url = test_config$armadillo_url, user = test_config$user, admin_pwd = test_config$admin_pwd, dest = test_config$dest, profile = test_config$profile, ADMIN_MODE = test_config$ADMIN_MODE)

cli_h2("Logging in as data manager")
cli_alert_info(sprintf("Login to %s", test_config$armadillo_url))

if(ADMIN_MODE) {
    armadillo.login_basic(test_config$armadillo_url, "admin", test_config$admin_pwd)
} else {
    armadillo.login(test_config$armadillo_url)
}
cli_alert_success("Logged in")

cli_h2("Creating a test project")
project1 <- generate_random_project_name()
create_test_project(project1)
cli_alert_success(paste0(project1, " created"))

cli_h2("Uploading test data")  # Add option for survival data?
source("test-cases/upload-data.R")
print(test_config$default_parquet_path)
upload_test_data(project = project1, dest = test_config$default_parquet_path)
cli_alert_success("Data uploaded")

cli_h2("Uploading resource source file")
source("test-cases/upload-resource.R")
upload_resource(project = project1, rda_dir = test_config$rda_dir, url = test_config$armadillo_url, token = token, auth_type = test_config$auth_type, skip_tests = test_config$skip_tests)
cli_alert_success("Resource source file uploaded")
# #
# # cli_h2("Creating resource")
# source("test-cases/create-resource.R")
# resGSE1 <- make_resource(project1)
# cli_alert_success("Resource created")
#
# cli_h2("Uploading resource file")
# armadillo.upload_resource(project = project1, folder = "ewas", resource = resGSE1, name = "GSE66351_1")
# cli_alert_success("Resource uploaded")
#
# cli_h2("Creating linked view on table")
# source("test-cases/create-linked-view.R")
# cli_alert_success("Linked view created")
#
# cli_h2("Starting manual UI test")
# source("test-cases/manual-test.R")
# interactive_test(project1)
# cli_alert_success("Manual test complete")
#
# cli_alert_info("\nNow you're going to test as researcher")
# cli_h2("Setting researcher permissions")
# source("test-cases/set_researcher_access.R")
# set_researcher_access(required_projects = list(project1)) #Add linked table when working
# cli_alert_success("Researcher permissions set")
#
# cli_h2("Logging in as a researcher")
# logindata <- create_dsi_builder(url = armadillo_url, profile = profile, password = admin_pwd, token = token, table = sprintf("%s/2_1-core-1_0/nonrep", project1))
# cli_alert_info(sprintf("Login with profile [%s] and table: [%s/2_1-core-1_0/nonrep]", profile, project1))
# conns <- datashield.login(logins = logindata, symbol = "core_nonrep", variables = c("coh_country"), assign = TRUE)
# cli_alert_success("Logged in")
#
# cli_h2("Verifying connecting to profiles possible")
# source("test-cases/verify-profile.R")
# verify_profiles(password = admin_pwd, token = token, url = armadillo_url, profile = profile)
# cli_alert_success("Profiles work")
#
# cli_h2("Assigning tables as researcher")
# source("test-cases/assigning.R")
# check_tables_assign(project = project1, folder = "2_1-core-1_0", table = "nonrep")
# check_expression_assign(project = project1, object = "nonrep", variable = "coh_country")
# cli_alert_success("Assigning works")
#
# # cli_h2("Testing linked table")
# # source("test-cases/test-linked-view.R")
# # cli_alert_success("Linked view worked")
#
# cli_h2("Testing resources as a researcher")
# if (ADMIN_MODE) {
#    cli_alert_warning("Cannot test working with resources as basic authenticated admin")
# } else if (!"resourcer" %in% profile_info$packageWhitelist) {
#   cli_alert_warning(sprintf("Resourcer not available for profile: %s, skipping testing using resources.", profile))
# } else {
#     cli_h2("Using resources as regular user")
#     cli_h2("Verifying resources")
#     source("test-cases/verify-resources.R")
#     verify_resources(project = project1, resource_path = "ewas/GSE66351_1")
# }
#
# cli_h2("Verifying xenon packages")
# cli_alert_info("Verifying dsBase")
# source("test-cases/ds-base.R")
# verify_ds_base(object = "nonrep", variable = "coh_country")
# cli_alert_success("dsBase works")
#
# cli_alert_info("Verifying dsMediation")
# source("test-cases/xenon-mediate.R")
# verify_ds_mediation()
# cli_alert_success("dsMediation works")
#
# cli_alert_info("Testing dsSurvival")
# source("test-cases/xenon-survival.R")
# run_survival_tests(project = project1, data_path = "/survival/veteran", conns = conns)
# cli_alert_success("dsSurvival works")
#
# cli_alert_info("Testing dsMTL")
# source("test-cases/xenon-mtl.R")
#
# cli_alert_info("Logging in as two cohorts")
# logindata_1 <- create_dsi_builder(server = "testserver1", url = armadillo_url, profile = profile, password = admin_pwd, token = token, table = sprintf("%s/2_1-core-1_0/nonrep", project1))
# logindata_2 <- create_dsi_builder(server = "testserver2", url = armadillo_url, profile = profile, password = admin_pwd, token = token, table = sprintf("%s/2_1-core-1_0/nonrep", project1))
# logindata <- rbind(logindata_1, logindata_2) #This allows us to test two servers (required for dsMTL)
# conns <- DSI::datashield.login(logins = logindata, assign = T, symbol = "nonrep")
#
# verify_ds_mtl()
# cli_alert_success("dsMTL works")
#
# cli_h2("Removing data as admin")
# source("test-cases/remove-data.R") #Add link_project once module works
# dm_clean_up(user, admin_pwd, required_projects = project1)
# cli_alert_success("Successfully removed data as admin")
# datashield.logout(conns)

# NOT SURE WHAT THIS DOES OR ADDS TO PREVIOUS TESTS
# project2 <- generate_random_project_name(available_projects)
# available_projects <- c(available_projects, project2)
# if(admin_pwd != "") {
#   cli_h2("Basic authentication")
#   cli_alert_info("Logging in as admin user")
#   armadillo.login_basic(armadillo_url, "admin", admin_pwd)
#   cli_alert_info(sprintf("Creating project [%s]", project2))
#   armadillo.create_project(project2)
#   nonrep <- arrow::read_parquet(paste0(dest, "core/nonrep.parquet"))
#   cli_alert_info(sprintf("Uploading file to [%s]", project2))
#   armadillo.upload_table(project2, "2_1-core-1_0", nonrep)
#   rm(nonrep)
#   check_cohort_exists(project2)
#   table <- sprintf("%s/2_1-core-1_0/nonrep", project2)
#   if(table %in% armadillo.list_tables(project2)){
#     cli_alert_success(paste0(table, " exists"))
#   } else {
#     exit_test(paste0(table, " doesn't exist"))
#   }
#   cli_alert_info(sprintf("Deleting [%s]", project2))
#   armadillo.delete_project(project2)
# } else {
#   cli_alert_warning("Testing basic authentication skipped, admin password not available")
# }
#
cli_alert_info("Testing done")
cli_alert_info("Please test rest of UI manually, if impacted this release")
