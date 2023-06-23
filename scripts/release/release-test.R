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
# armadillo/datashield libraries needed for testing
library(MolgenisArmadillo)
library(DSI)
library(dsBaseClient)
library(DSMolgenisArmadillo)
library(resourcer)

# set when admin password given + question answered with y
update_auto = ""
do_run_spinner <- TRUE

exit_test <- function(msg){
  cond = structure(list(message=msg), class=c("exit", "condition"))
  signalCondition(cond)
}

create_bearer_header <- function(token){
  return(paste0("Bearer ", token))
}

create_basic_header <- function(pwd){
  encoded <- base64enc::base64encode(
    charToRaw(
      paste0("admin:", pwd))
  )
  return(paste0("Basic ", encoded))
}

# make authentication header for api calls, basic or bearer based on type
get_auth_header <- function(type, key){
  header_content <- ""
  if(tolower(type) == "bearer"){
    header_content <- create_bearer_header(key)
  } else if(tolower(type) == "basic") {
    header_content <- create_basic_header(key)
  } else {
    exit_test(sprintf("Type [%s] invalid, choose from 'basic' and 'bearer'"))
  }
  return(c("Authorization" = header_content))
}

# armadillo api put request
put_to_api <- function(endpoint, key, auth_type, body_args){
  auth_header <- get_auth_header(auth_type, key)
  body <- jsonlite::toJSON(body_args, auto_unbox=TRUE)
  response <- PUT(paste0(url, endpoint), body=body, encode="json",
                  config = c(httr::content_type_json(), httr::add_headers(auth_header)))
  return(response)
}

spin_till_done <- function(spinner){
    # run_spinner is a boolean set on top of this script, it is set to false when loading is done and spinner can stop
    if (do_run_spinner) {
        Sys.sleep(0.1)
    } else {
        spinner$finish()
    }
}

run_spinner <- function(spinner) {
  lapply(1:1000, function(x) { spinner$spin(); spin_till_done(spinner)})
}

# post resource to armadillo api
post_resource_to_api <- function(project, key, auth_type, file, folder, name){
  auth_header <- get_auth_header(auth_type, key)
  plan(multisession)
  spinner <- make_spinner()
  # Do async call
  api_call <- future(POST(paste0(url, sprintf("storage/projects/%s/objects", project)), body=list(file = file, object=paste0(folder,"/", name)),
                    config = c(httr::add_headers(auth_header))))
  # Run spinner while waiting for response
  ansi_with_hidden_cursor(run_spinner(spinner))
  # Response will come when ready
  response <- value(api_call)
  # Set do_run_spinner to false, causing the spinner to stop running, see spin_till_done method
  do_run_spinner <- FALSE
  if(response$status_code != 204) {
    cli_alert_warning(sprintf("Could not upload [%s] to project [%s]", name, project))
    exit_test(content(response)$message)
  }
}

# get request to armadillo api without authentication
get_from_api <- function(endpoint) {
  response <- GET(paste0(url, endpoint))
  return(content(response))
}

# get request to armadillo api with an authheader
get_from_api_with_header <- function(endpoint, key, auth_type){
  auth_header <- get_auth_header(auth_type, key)
  response <- GET(paste0(url, endpoint), config = c(httr::add_headers(auth_header)))
  if(response$status_code != 200) {
    cli_alert_warning(sprintf("Cannot retrieve data from endpoint [%s]", endpoint))
  }
  return(content(response))
}

# add/edit user using armadillo api
set_user <- function(user, admin_pwd, isAdmin){
  args <- list(email = user, admin = isAdmin, projects= list("cohort1", "omics"))
  response <- put_to_api("access/users", admin_pwd, "basic", args)
  if(response$status_code != 204) {
    cli_alert_warning("Altering OIDC user failed, please do this manually")
    update_auto = ""
  }
}

wait_for_input <- function(){
  cat("\nPress any key to continue")
  continue <- readLines("stdin", n=1)
}

# log version info of loaded libraries
show_version_info <- function(libs){
  libs_to_print <- cli_ul()
  for (i in 1:length(libs)) {
    lib = libs[i]
    cli_li(sprintf("%s: %s\n", lib, packageVersion(lib)))
  }
  cli_end(libs_to_print)
}

create_dir_if_not_exists <- function(directory){
  if (!dir.exists(paste0(dest, directory))) {
    dir.create(paste0(dest, directory))
  }
}

add_slash_if_not_added <- function(path){
  if (substr(dest, nchar(path), nchar(path)) != "/"){
    return(paste0(path, "/"))
  } else {
    return(path)
  }
}

# theres a bit of noise added in DataSHIELD answers, causing calculations to not always be exactly the same, but close
# here we check if they're equal enough
almost_equal <- function(val1, val2) {
  return(all.equal(val1, val2, tolerance= .Machine$double.eps ^ 0.3))
}

# compare values in two lists
compare_list_values <- function(list1, list2) {
  vals_to_print <- cli_ul()
  equal <- T
  for (i in 1:length(list1)) {
    val1 = list1[i]
    val2 = list2[i]
    if(almost_equal(val1, val2)){
      cli_li(sprintf("%s ~= %s", val1, val2))
    } else {
      equal <- F
      cli_li(sprintf("%s != %s", val1, val2))
    }
  }
  cli_end(vals_to_print)
  if(!equal){
    cli_alert_danger("Not equal!")
  }
}

print_list <- function(list){
  vals_to_print <- cli_ul()
  for (i in 1:length(list)) {
    val = list[i]
    cli_li(val)
  }
  cli_end(vals_to_print)
}

check_cohort_exists <- function(cohort){
  if(cohort %in% armadillo.list_projects()){
    cli_alert_success(paste0(cohort, " exists"))
  } else {
    cli_alert_danger(paste0(cohort, "doesn't exist"))
    exit_test(paste0(cohort, " doesn't exist!"))
  }
}

download_test_files <- function(urls, dest){
  n_files <- length(urls)
  cli_progress_bar("Downloading testfiles", total = n_files)
  for (i in 1:n_files) {
    url <- urls[i]
    splitted <- strsplit(url, "/")[[1]]
    folder <- splitted[length(splitted) - 1]
    filename <- splitted[length(splitted)]
    cli_alert_info(paste0("Downloading ", filename))
    download.file(url, paste0(dest, folder, "/", filename), quiet=TRUE)
    cli_progress_update()
  }
  cli_progress_done()
}

# here we start the script chronologically
cli_alert_success("Loaded Armadillo/DataSHIELD libraries:")
show_version_info(c("MolgenisArmadillo", "DSI", "dsBaseClient", "DSMolgenisArmadillo", "resourcer"))

cli_alert_success("Loaded other libraries:")
show_version_info(c("getPass", "arrow", "httr", "jsonlite", "future"))

cat("\nEnter URL of testserver (for default https://armadillo-demo.molgenis.net/, press enter): ")
url <- readLines("stdin", n=1)

if(url == ""){
  url = "https://armadillo-demo.molgenis.net/"
}

cat("Location of molgenis-service-armadillo on your PC (for default ~/git/, press enter, if not available press x): ")
service_location <- readLines("stdin", n=1)
dest <- ""

cat("Enter password for admin user (basic auth)\n")
admin_pwd<- getPass::getPass()

cat("Enter your OIDC username (email): ")
user <- readLines("stdin", n=1)

if(service_location == "") {
  dest = "~/git/molgenis-service-armadillo/data/shared-lifecycle/"
} else if (service_location == "x") {
  cat("Do you want to download the files? (y/n) ")
  download <- readLines("stdin", n=1)
  if (download == "y") {
    cat("Where do you want to download the files? ")
    dest <- readLines("stdin", n=1)
    if (dir.exists(dest)) {
      cli_alert_info(paste0("Downloading test files into: ", dest))
      dest <- add_slash_if_not_added(dest)
      create_dir_if_not_exists("core")
      create_dir_if_not_exists("outcome")
      test_files_url_template <- "https://github.com/molgenis/molgenis-service-armadillo/raw/master/data/shared-lifecycle/%s/%srep.parquet"
      download_test_files(
        c(
        sprintf(test_files_url_template, "core", "non"),
        sprintf(test_files_url_template, "core", "yearly"),
        sprintf(test_files_url_template, "core", "monthly"),
        sprintf(test_files_url_template, "core", "trimester"),
        sprintf(test_files_url_template, "outcome", "non"),
        sprintf(test_files_url_template, "outcome", "yearly")
        ),
        dest
      )
    } else {
      exit_test(sprintf("Release test halted: Directory [%s] doesn't exist", dest))
    }
  } else {
    exit_test("Release test halted: No test files available")
  }
} else {
  service_location <- add_slash_if_not_added(service_location)
  dest <- paste0(service_location, "/molgenis-service-armadillo/data/shared-lifecycle/")
  if (!dir.exists(dest)) {
    exit_test(sprintf("Release test halted: Directory [%s] doesn't exist", service_location))
  }
}

cat("Do you have testfile [gse66351_1.rda] downloaded? (y/n) ")
rda_available = readLines("stdin", n=1)
rda_dir = ""
test_resource = "gse66351_1.rda"
if (rda_available == "y"){
  cat(sprintf("Specify path to %s (including filename): ", test_resource))
  rda_dir = readLines("stdin", n=1)
} else {
  cat("Where do you want to download it: ")
  download_dir = readLines("stdin", n=1)
  download_dir <- add_slash_if_not_added(download_dir)
  if(dir.exists(download_dir)){
    cli_alert_info(sprintf("Downloading %s into %s", test_resource, download_dir))
    download.file("https://github.com/isglobal-brge/brge_data_large/raw/master/data/gse66351_1.rda", paste0(download_dir, test_resource))
    rda_dir = paste0(download_dir, test_resource)
  }
}

if (rda_dir == "" || !dir.exists(rda_dir)) {
  exit_test(sprintf("Directory [%s] doesn't exist", rda_dir))
}

app_info <- get_from_api("actuator/info")

version <- unlist(app_info$build$version)

cat("\nAvailable profiles: \n")
profiles <- get_from_api("profiles")
print_list(unlist(profiles$available))

cat("Which profile do you want to test on? (press enter to continue using xenon) ")
profile <- readLines("stdin", n=1)
if (profile == "") {
  profile <- "xenon"
}

cli_alert_info("Checking if profile is prepared for all tests")
token <- armadillo.get_token(url)
profile_info <- get_from_api_with_header(paste0("ds-profiles/", profile), token, "bearer")
seed <- unlist(profile_info$options$datashield.seed)
whitelist <- unlist(profile_info$packageWhitelist)
if(is.null(seed)){
  cli_alert_warning(sprintf("Seed of profile [%s] is NULL, please set it in UI profile tab and restart the profile", profile))
  wait_for_input()
}
if(!"resourcer" %in% whitelist){
  cli_alert_warning(sprintf("Whitelist of profile [%s] does not contain resourcer, please add it and restart the profile", profile))
  wait_for_input()
}

if(!is.null(seed) && "resourcer" %in% whitelist){
  cli_alert_success(sprintf("Profile [%s] okay for testing", profile))
}

cli_h1("Release test")
cat(sprintf("
                  ,.-----__                       Testing version: %s
            ,:::://///,:::-.                      Test server: %s
           /:''/////// ``:::`;/|/                 OIDC User: %s
          /'   ||||||     :://'`\\                 Admin password set: %s
        .' ,   ||||||     `/(  e \\                Directory for test files: %s
  -===~__-'\\__X_`````\\_____/~`-._ `.              Profile: %s
              ~~        ~~       `~-'
", version, url, user, admin_pwd != "", dest, profile))

cli_h2("Table upload")
cli_alert_info(sprintf("Login to %s", url))
armadillo.login(url)
cli_alert_info("Creating project cohort1")
armadillo.create_project("cohort1")
cli_alert_info("Checking if project 'cohort1' exists")
check_cohort_exists("cohort1")

cli_alert_info("Reading parquet files for core variables")
cli_alert_info("core/nonrep")
nonrep <- arrow::read_parquet(paste0(dest, "core/nonrep.parquet"))
cli_alert_success("core/nonrep read")
cli_alert_info("core/yearlyrep")
yearlyrep <- arrow::read_parquet(paste0(dest, "core/yearlyrep.parquet"))
cli_alert_success("core/yearlyrep read")
cli_alert_info("core/monthlyrep")
monthlyrep <- arrow::read_parquet(paste0(dest, "core/monthlyrep.parquet"))
cli_alert_success("core/monthlyrep read")
cli_alert_info("core/trimesterrep")
trimesterrep <- arrow::read_parquet(paste0(dest, "core/trimesterrep.parquet"))
cli_alert_success("core/trimesterrep read")

cli_alert_info("Uploading core test tables")
armadillo.upload_table("cohort1", "2_1-core-1_0", nonrep)
armadillo.upload_table("cohort1", "2_1-core-1_0", yearlyrep)
armadillo.upload_table("cohort1", "2_1-core-1_0", monthlyrep)
armadillo.upload_table("cohort1", "2_1-core-1_0", trimesterrep)
cli_alert_success("Uploaded files into core")

rm(nonrep, yearlyrep, monthlyrep, trimesterrep)

cli_alert_info("Reading parquet files for outcome variables")
nonrep <- arrow::read_parquet(paste0(dest, "outcome/nonrep.parquet"))
yearlyrep <- arrow::read_parquet(paste0(dest, "outcome/yearlyrep.parquet"))

cli_alert_info("Uploading outcome test tables")
armadillo.upload_table("cohort1", "1_1-outcome-1_0", nonrep)
armadillo.upload_table("cohort1", "1_1-outcome-1_0", yearlyrep)
cli_alert_success("Uploaded files into outcome")

cli_alert_info("Checking if colnames of trimesterrep available")
trimesterrep <- armadillo.load_table("cohort1", "2_1-core-1_0", "trimesterrep")
cols <- c("row_id","child_id","age_trimester","smk_t","alc_t")
if (identical(colnames(trimesterrep), cols)){
  cli_alert_success("Colnames correct")
} else {
  cli_alert_danger(paste0(colnames(trimesterrep), "!=", cols))
  exit_test("Colnames incorrect")
}

cli_h2("Manual test UI")
cat("\nNow open your testserver in the browser")
cat("\n\nVerify 'cohort1' is available")
wait_for_input()
cat("\nClick on the icon next to the name to go to the project explorer")
wait_for_input()
cat("\nVerify the 1_1-outcome-1_0 and 2_1-core-1_0 folders are there")
wait_for_input()
cat("\nVerify core contains nonrep, yearlyrep, monthlyrep and trimesterrep")
wait_for_input()
cat("\nVerify outcome contains nonrep and yearlyrep")
wait_for_input()

cat("\nWere the manual tests successful? (y/n) ")
success <- readLines("stdin", n=1)
if(success != "y"){
  cli_alert_danger("Manual tests failed: problem in UI")
  exit_test("Some values incorrect in UI projects view")
}

cli_h2("Resource upload")
cli_alert_info("Creating project omics")
armadillo.create_project("omics")
rda_file_body <- upload_file(rda_dir)
cli_alert_info(sprintf("Uploading resource file to %s into project [%s]", url, "omics"))
post_resource_to_api("omics", token, "bearer", rda_file_body, "ewas", "gse66351_1.rda")

cli_alert_info("Creating resource")
resGSE1 <- resourcer::newResource(
  name = "GSE66351_1",
  url = "https://armadillo-demo.molgenis.net/storage/projects/omics/objects/ewas%2Fgse66351_1.rda",
  format = "ExpressionSet"
)
cli_alert_info("Uploading RDS file")
armadillo.upload_resource(project="omics", folder="ewas", resource = resGSE1, name = "GSE66351_1")

cli_alert_info("\nNow you're going to test as researcher")
if(admin_pwd != ""){
  cat("\nDo you want to remove admin from OIDC user automatically? (y/n) ")
  update_auto <- readLines("stdin", n=1)
  if(update_auto == "y"){
    set_user(user, admin_pwd, F)
  }
}

if(update_auto != "y"){
  cat("\nGo to the Users tab")
  cat("\nAdd 'cohort1' and 'omics' to the project column for your account")
  cat("\nRevoke your admin permisions\n")
  cli_alert_warning("Make sure you either have the basic auth admin password or someone available to give you back your permissions")
  wait_for_input()
}

cli_h2("Using tables as regular user")
cli_alert_info("Retrieving token")
token <- armadillo.get_token(url)
cli_alert_info("Creating new builder")
builder <- DSI::newDSLoginBuilder()
cli_alert_info("Append information to builder")
builder$append(server = "armadillo",
               url = url,
               profile="xenon",
               token = token,
               table = "cohort1/2_1-core-1_0/nonrep",
               driver = "ArmadilloDriver")
cli_alert_info("Building")
logindata <- builder$build()

cli_alert_info(paste0("Login with profile [", profile, "] and table: [cohort1/2_1-core-1_0/nonrep]"))
conns <- datashield.login(logins = logindata, symbol = "core_nonrep", variables = c("coh_country"), assign = TRUE)
cli_alert_info("Assigning table core_nonrep")
datashield.assign.table(conns, "core_nonrep", "cohort1/2_1-core-1_0/nonrep")
cli_alert_info("Assigning expression for core_nonrep$coh_country")
datashield.assign.expr(conns, "x", expr=quote(core_nonrep$coh_country))
cli_alert_info("Verifying connecting to profile possible")
con <- dsConnect(
  drv = armadillo(),
  name = "armadillo",
  token = token,
  url = url,
  profile = profile,
)
cli_alert_info("Verifying mean function works on core_nonrep$country")
ds_mean <- ds.mean("core_nonrep$coh_country", datasources = conns)$Mean
cli_alert_info("Verifying values")
if(! ds_mean[1] == 431.105) {
  cli_alert_danger(paste0(ds_mean[1], "!=", 431.105))
  exit_test("EstimatedMean incorrect!")
} else if (ds_mean[2] != 0) {
  cli_alert_danger(paste0(ds_mean[2], "!=", 0))
  exit_test("Nmissing incorrect!")
} else if (ds_mean[3] != 1000) {
  cli_alert_danger(paste0(ds_mean[3], "!=", 1000))
  exit_test("Nvalid incorrect!")
} else if (ds_mean[4] != 1000) {
  cli_alert_danger(paste0(ds_mean[4], "!=", 1000))
  exit_test("Ntotal incorrect!")
}
cli_alert_success("Mean values correct")
cli_alert_info("Verifying can create histogram")
hist <- ds.histogram(x = "core_nonrep$coh_country", datasources = conns)
cli_alert_info("Verifying values in histogram")

breaks <- c(35.31138,116.38319,197.45500,278.52680,359.59861,440.67042,521.74222,602.81403,683.88584,764.95764,846.02945)
counts <- c(106,101,92,103,106,104,105,101,113,69)
density <- c(0.0013074829,0.0012458092,0.0011347965,0.0012704787,0.0013074829,0.0012828134,0.0012951481,0.0012458092,0.0013938261,0.0008510974)
mids <- c(75.84729,156.91909,237.99090,319.06271,400.13451,481.20632,562.27813,643.34993,724.42174,805.49355)
cli_alert_info("Validating histogram breaks")
compare_list_values(hist$breaks, breaks)
cli_alert_info("Validating histogram counts")
compare_list_values(hist$counts, counts)
cli_alert_info("Validating histogram density")
compare_list_values(hist$density, density)
cli_alert_info("Validating histogram mids")
compare_list_values(hist$mids, mids)

cli_h2("Using resources as regular user")
cli_alert_info("Testing whether we can retrieve resources properly")
builder <- DSI::newDSLoginBuilder()
builder$append(
  server = "testserver",
  url = url,
  token = token,
  driver = "ArmadilloDriver",
  profile = profile,
  resource = "omics/ewas/GSE66351_1"
)
login_data <- builder$build()
conns <- DSI::datashield.login(logins = login_data, assign = TRUE)
cli_alert_info("Testing if we see the resource")
datashield.resources(conns = conns)
cli_alert_info("Testing if we can assign resource")
datashield.assign.resource(conns, resource="omics/ewas/GSE66351_1", symbol="eSet_0y_EUR")
cli_alert_info("Setting class")
ds.class('eSet_0y_EUR', datasources = conns)
cli_alert_info("Testing if we can assign expression")
datashield.assign.expr(conns, symbol = "methy_0y_EUR",expr = quote(as.resource.object(eSet_0y_EUR)))

cli_h2("Default profile")
cli_alert_info("Verify if default profile works without specifying profile")
con <- dsConnect(
  drv = armadillo(),
  name = "armadillo",
  token = token,
  url = url
)

cli_alert_info("Verify if default profile works when specifying profile")
con <- dsConnect(
  drv = armadillo(),
  name = "armadillo",
  token = token,
  profile = "default",
  url = url
)

cli_h2("Removing data as admin")
cat("We're now continueing with the datamanager workflow as admin\n")
if(update_auto == "y"){
  set_user(user, admin_pwd, T)
} else{
  cat("Make your account admin again")
  wait_for_input()
}
armadillo.delete_table("cohort1", "2_1-core-1_0", "nonrep")
armadillo.delete_table("cohort1", "2_1-core-1_0", "yearlyrep")
armadillo.delete_table("cohort1", "2_1-core-1_0", "trimesterrep")
armadillo.delete_table("cohort1", "2_1-core-1_0", "monthlyrep")
armadillo.delete_table("cohort1", "1_1-outcome-1_0", "nonrep")
armadillo.delete_table("cohort1", "1_1-outcome-1_0", "yearlyrep")
cat("\nVerify in UI all data from cohort1 is gone.")
wait_for_input()
armadillo.delete_project("cohort1")
cat("\nVerify in UI project cohort1 is gone")
wait_for_input()
armadillo.delete_project("omics")
cat("\nVerify in UI project omics is gone")
wait_for_input()

if(admin_pwd != ""){
  cli_h2("Basic authentication")
  cli_alert_info("Logging in as admin user")
  armadillo.login_basic(url, "admin", admin_pwd)
  cli_alert_info("Creating project 'cohort2'")
  armadillo.create_project("cohort2")
  nonrep <- arrow::read_parquet("~/git/molgenis-service-armadillo/data/shared-lifecycle/core/nonrep.parquet")
  cli_alert_info("Uploading file to 'cohort2'")
  armadillo.upload_table("cohort2", "2_1-core-1_0", nonrep)
  rm(nonrep)
  check_cohort_exists("cohort2")
  table <- "cohort2/2_1-core-1_0/nonrep"
  if(table %in% armadillo.list_tables("cohort2")){
    cli_alert_success(paste0(table, " exists"))
  } else {
    cli_alert_danger(paste0(table, " doesn't exist"))
    exit_test(paste0(table, " doesn't exist"))
  }
  cli_alert_info("Deleting 'cohort2'")
  armadillo.delete_project("cohort2")
} else {
  cli_alert_warning("Testing basic authentication skipped, admin password not available")
}

cli_alert_info("Testing done")
cli_alert_info("Please test rest of UI manually, if impacted this release")
