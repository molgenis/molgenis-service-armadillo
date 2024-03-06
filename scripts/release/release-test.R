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
source("common-functions.R")
cli_alert_success("Functions loaded")

#
# create_dir_if_not_exists <- function(directory){
#   if (!dir.exists(paste0(dest, directory))) {
#     dir.create(paste0(dest, directory))
#   }
# }
#
# download_test_files <- function(urls, dest){
#   n_files <- length(urls)
#   cli_progress_bar("Downloading testfiles", total = n_files)
#   for (i in 1:n_files) {
#     download_url <- urls[i]
#     splitted <- strsplit(download_url, "/")[[1]]
#     folder <- splitted[length(splitted) - 1]
#     filename <- splitted[length(splitted)]
#     cli_alert_info(paste0("Downloading ", filename))
#     download.file(download_url, paste0(dest, folder, "/", filename), quiet=TRUE)
#     cli_progress_update()
#   }
#   cli_progress_done()
# }
#
# generate_random_project_seed <- function(current_project_seeds) {
#   random_seed <- round(runif(1, min = 100000000, max=999999999))
#   if (!random_seed %in% current_project_seeds) {
#     return(random_seed)
#   } else {
#     generate_random_project_seed(current_project_seeds)
#   }
# }
#
# generate_project_port <- function(current_project_ports) {
#   starting_port <- 6312
#   while (starting_port %in% current_project_ports) {
#     starting_port = starting_port + 1
#   }
#   return(starting_port)
# }
#
# obtain_existing_profile_information <- function(key, auth_type) {
#   responses <- get_from_api_with_header('ds-profiles', key, auth_type)
#   response_df <- data.frame(matrix(ncol=5,nrow=0, dimnames=list(NULL, c("name", "container", "port", "seed", "online"))))
#   for (response in responses) {
#     if("datashield.seed" %in% names(response$options)) {
#       datashield_seed <- response$options$datashield.seed
#     } else {
#       datashield_seed <- NA
#     }
#
#     response_df[nrow(response_df) + 1,] = c(response$name, response$image, response$port, datashield_seed, response$container$status)
#   }
#   return(response_df)
# }
#
# start_profile <- function(profile_name, key, auth_type) {
#   auth_header <- get_auth_header(auth_type, key)
#   cli_alert_info(sprintf('Attempting to start profile: %s', profile_name))
#   response <- POST(
#     sprintf("%sds-profiles/%s/start", armadillo_url, profile_name),
#     config = c(httr::add_headers(auth_header))
#     )
#   if (!response$status_code == 204) {
#     exit_test(sprintf("Unable to start profile %s, error code: %s", profile_name, response$status_code))
#   } else {
#     cli_alert_success(sprintf("Successfully started profile: %s", profile_name))
#   }
# }
#
# return_list_without_empty <- function(to_empty_list) {
#   return(to_empty_list[to_empty_list != ''])
# }
#
# create_profile <- function(profile_name, key, auth_type) {
#   if (profile_name %in% profile_defaults$name) {
#     cli_alert_info(sprintf("Creating profile: %s", profile_name))
#     profile_default <- profile_defaults[profile_defaults$name == profile_name,]
#     current_profiles <- obtain_existing_profile_information(key, auth_type)
#     new_profile_seed <- generate_random_project_seed(current_profiles$seed)
#     whitelist <- as.list(stri_split_fixed(paste("dsBase", profile_default$whitelist, sep = ","), ",")[[1]])
#     blacklist <- as.list(stri_split_fixed(profile_default$blacklist, ",")[[1]])
#     port <- profile_default$port
#     if (port == "") {
#       port <- generate_project_port(current_profiles$port)
#     }
#     args <- list(
#       name = profile_name,
#       image = profile_default$container,
#       host = "localhost",
#       port = port,
#       packageWhitelist = return_list_without_empty(whitelist),
#       functionBlacklist = return_list_without_empty(blacklist),
#       options = list(datashield.seed = new_profile_seed)
#     )
#     response <- put_to_api('ds-profiles', key, auth_type, body_args = args)
#     if (response$status_code == 204) {
#       cli_alert_success(sprintf("Profile %s successfully created.", profile_name))
#       start_profile(profile_name, key, auth_type)
#     } else {
#       exit_test(sprintf("Unable to create profile: %s , errored %s", profile_name, response$status_code))
#     }
#   } else {
#     exit_test(sprintf("Unable to create profile: %s , unknown profile", profile_name))
#   }
# }
#

#


# here we start the script chronologically
cli_h2("Configuring test options")
source("test-config.R")
cli_alert_success("Options configured")

cli_h2("Preparing tables for tests")
source("download-tables.R")
cli_alert_success("Tables ready for testing")

cli_h2("Preparing resource for tests")
source("download-resources.R")
cli_alert_success("Resource ready for testing")

cli_h2("Determining whether to run with password or token")
source("set-admin-mode.R")
cli_alert_success("Permissions set")

cli_h2("Configuring profiles")
source("setup-profiles.R")
cli_alert_success("Profiles configured")

cli_h1("Starting release test")
source("release-test-info.R")

cli_h2("Logging in as data manager")
cli_alert_info(sprintf("Login to %s", armadillo_url))
if(ADMIN_MODE) {
    armadillo.login_basic(armadillo_url, "admin", admin_pwd)
} else {
    armadillo.login(armadillo_url)
} # Do we need these two log ins?
cli_alert_success("Logged in")

cli_h2("Creating a test project")
project1 <- generate_random_project_name()
create_test_project(project1)
cli_alert_success(paste0(project1, " created"))

cli_h2("Starting manual UI test")
source("manual-test.R")
interactive_test(project1)
cli_alert_success("Manual test complete")

cli_h2("Uploading test data")  # Add option for survival data?
source("upload-data.R")
cli_alert_success("Data uploaded")

# cli_h2("Uploading resource source file")
# source("test-cases/upload-resource.R")
# upload_resource(project1)
# cli_alert_success("Resource source file uploaded")
#
# cli_h2("Creating resource")
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

cli_alert_info("\nNow you're going to test as researcher")
cli_h2("Setting researcher permissions")
source("test-cases/set_researcher_access.R")
set_researcher_access(required_projects = project1)
cli_alert_success("Researcher permissions set")

cli_h2("Logging in as a researcher")
logindata <- create_dsi_builder(url = armadillo_url, profile = profile, password = admin_pwd, token = token, table = sprintf("%s/2_1-core-1_0/nonrep", project1))
cli_alert_info(sprintf("Login with profile [%s] and table: [%s/2_1-core-1_0/nonrep]", profile, project1))
conns <- datashield.login(logins = logindata, symbol = "core_nonrep", variables = c("coh_country"), assign = TRUE)
cli_alert_success("Logged in")

cli_h2("Verifying connecting to profile possible")
source("test-cases/verify-profile.R")
verify_profile(password = admin_pwd, token = token, url = armadillo_url, profile = profile)
cli_alert_success("Profile works")

cli_h2("Assigning tables as researcher")
source("test-cases/assigning.R")
check_tables_assign(project = project1, folder = "2_1-core-1_0", table = "nonrep")
check_expression_assign(project = project1, object = "nonrep", variable = "coh_country")
cli_alert_success("Assigning works")

# cli_h2("Testing linked table")
# source("test-cases/test-linked-view.R")
# cli_alert_success("Linked view worked")

cli_h2("Verifying xenon packages")
cli_alert_info("Verifying dsBase")
source("test-cases/ds-base.R")
verify_ds_base(object = "nonrep", variable = "coh_country")
cli_alert_success("dsBase works")

cli_alert_info("Verifying dsMediation")
source("test-cases/xenon-mediate.R")
cli_alert_success("dsMediation works")

cli_alert_info("Testing dsSurvival")
source("test-cases/xenon-survival.R")
run_survival_tests(project = project1, data_path = "/survival/veteran", conns = conns)
cli_alert_success("dsSurvival works")
datashield.logout(conns)

cli_alert_info("Testing dsMTL")
source("test-cases/xenon-mtl.R")
cli_alert_info("Logging in as two cohorts")
logindata_1 <- create_dsi_builder(server = "testserver1", url = armadillo_url, profile = profile, password = admin_pwd, token = token, table = sprintf("%s/2_1-core-1_0/nonrep", project1))
logindata_2 <- create_dsi_builder(server = "testserver2", url = armadillo_url, profile = profile, password = admin_pwd, token = token, table = sprintf("%s/2_1-core-1_0/nonrep", project1))
logindata <- rbind(logindata_1, logindata_2) #This allows us to test two servers (required for dsMTL)
conns <- DSI::datashield.login(logins = logindata, assign = T, symbol = "nonrep")

verify_ds_mtl()
cli_alert_success("dsMTL works")
datashield.logout(conns)

#
# if (ADMIN_MODE) {
#    cli_alert_warning("Cannot test working with resources as basic authenticated admin")
# } else if (!"resourcer" %in% profile_info$packageWhitelist) {
#   cli_alert_warning(sprintf("Resourcer not available for profile: %s, skipping testing using resources.", profile))
# } else {
#     cli_h2("Using resources as regular user")
#
#     login_data <- create_dsi_builder(server = "testserver", url = armadillo_url, token = token, profile = profile, resource = sprintf("%s/ewas/GSE66351_1", omics_project))
#     conns <- DSI::datashield.login(logins = login_data, assign = TRUE)
#
#     cli_alert_info("Testing if we see the resource")
#     resource_path <- sprintf("%s/ewas/GSE66351_1", omics_project)
#     if(datashield.resources(conns = conns)$testserver == resource_path){
#       cli_alert_success("Success")
#     } else {
#       cli_alert_danger("Failure")
#     }
#     cli_alert_info("Testing if we can assign resource")
#     datashield.assign.resource(conns, resource =resource_path, symbol="eSet_0y_EUR")
#     cli_alert_info("Getting RObject class of resource")
#     resource_class <- ds.class('eSet_0y_EUR', datasources = conns)
#     expected <- c("RDataFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
#     if (length(setdiff(resource_class$testserver, expected)) == 0) {
#       cli_alert_success("Success")
#     } else {
#       cli_alert_danger("Failure")
#     }
#     cli_alert_info("Testing if we can assign expression")
#     tryCatch({
#       datashield.assign.expr(conns, symbol = "methy_0y_EUR",expr = quote(as.resource.object(eSet_0y_EUR)))
#     }, error = function(e) {
#         cli_alert_danger(datashield.errors())
#     })
#     datashield.logout(conns)
# }
#
# cli_h2("Default profile")
# cli_alert_info("Verify if default profile works without specifying profile")
# con <- create_ds_connection(password = admin_pwd, token = token, url = armadillo_url)
# if (con@name == "armadillo") {
#   cli_alert_success("Succesfully connected")
# } else {
#   cli_alert_danger("Connection failed")
# }
# dsDisconnect(con)
#
# cli_alert_info("Verify if default profile works when specifying profile")
# con <- create_ds_connection(password = admin_pwd, token = token, url = armadillo_url, profile = "default")
# if (con@name == "armadillo") {
#   cli_alert_success("Succesfully connected")
# } else {
#   cli_alert_danger("Connection failed")
# }
# dsDisconnect(con)
#
# cli_h2("Removing data as admin")
# cat("We're now continueing with the datamanager workflow as admin\n")
# if(update_auto == "y"){
#   set_user(user, admin_pwd, T, project1, omics_project, link_project)
# } else{
#   cat("Make your account admin again")
#   wait_for_input()
# }
# armadillo.delete_table(project1, "2_1-core-1_0", "nonrep")
# armadillo.delete_table(project1, "2_1-core-1_0", "yearlyrep")
# armadillo.delete_table(project1, "2_1-core-1_0", "trimesterrep")
# armadillo.delete_table(project1, "2_1-core-1_0", "monthlyrep")
# armadillo.delete_table(project1, "1_1-outcome-1_0", "nonrep")
# armadillo.delete_table(project1, "1_1-outcome-1_0", "yearlyrep")
#
# cat(sprintf("\nVerify in UI all data from [%s] is gone.", project1))
# wait_for_input()
# armadillo.delete_project(project1)
# cat(sprintf("\nVerify in UI project [%s] is gone", project1))
# wait_for_input()
# armadillo.delete_project(omics_project)
# cat(sprintf("\nVerify in UI project [%s] is gone", omics_project))
# wait_for_input()
# armadillo.delete_project(link_project)
# cat(sprintf("\nVerify in UI project [%s] is gone", link_project))
# wait_for_input()
#
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
# cli_h3("Testing Rock profile")
# cli_alert_info(sprintf("Login to %s", armadillo_url))
# if(ADMIN_MODE) {
#   armadillo.login_basic(armadillo_url, "admin", token)
# } else {
#   armadillo.login(armadillo_url)
# }
# profile <- "rock"
# if (!as_docker_container) {
#   create_profile_if_not_available(profile, profiles$available, token, auth_type)
# }
# available_projects <- armadillo.list_projects()
# project3 <- generate_random_project_name(available_projects)
# available_projects <- c(available_projects, project3)
# cli_alert_info(sprintf("Creating project [%s]", project3))
# armadillo.create_project(project3)
# cli_alert_info(sprintf("Checking if project [%s] exists", project3))
# check_cohort_exists(project3)
#
# trimesterrep <- arrow::read_parquet(paste0(dest, "core/trimesterrep.parquet"))
# cli_alert_success("core/trimesterrep read")
#
# cli_alert_info("Uploading trimesterrep table")
# armadillo.upload_table(project3, "core", trimesterrep)
# rm(trimesterrep)
# cli_alert_success("Uploaded trimesterrep")
#
# cli_alert_info("Creating new builder")
# logindata <- create_dsi_builder(url = armadillo_url, profile = profile, password = admin_pwd, token = token, table = sprintf("%s/core/trimesterrep", project3))
#
# cli_alert_info(sprintf("Login with profile rock and table: [%s/core/trimesterrep]", project3))
# conns <- datashield.login(logins = logindata, symbol = "core_trimesterrep", variables = c("smk_t"), assign = TRUE)
#
# datashield.assign.table(conns, "core_trimesterrep", sprintf("%s/core/trimesterrep", project3))
#
# datashield.assign.expr(conns, "x", expr=quote(core_trimesterrep$smk_t))
#
# con <- create_ds_connection(password = admin_pwd, token = token, profile = profile, url = armadillo_url)
# if (con@name == "armadillo"){
#   cli_alert_success("Succesfully connected")
# } else {
#   cli_alert_danger("Connection failed")
# }
# dsDisconnect(con)
#
# ds_mean <- ds.mean("core_trimesterrep$smk_t", datasources = conns)$Mean
# datashield.logout(conns)
#
# cli_alert_info("Testing Rock profile mean values")
# verify_ds_obtained_mean(ds_mean, 61.059, 3000)
#
# armadillo.delete_table(project3, "core", "trimesterrep")
# armadillo.delete_project(project3)
# cat(sprintf("\nVerify in the UI that project [%s] and all its data is gone.", project3))
# wait_for_input()
#
# cli_alert_info("Testing done")
# cli_alert_info("Please test rest of UI manually, if impacted this release")
