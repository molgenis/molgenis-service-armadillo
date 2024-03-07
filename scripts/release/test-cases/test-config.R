# # log version info of loaded libraries
show_version_info <- function(libs) {
  libs_to_print <- cli_ul()
  for (i in 1:length(libs)) {
    lib = libs[i]
    cli_li(sprintf("%s: %s\n", lib, packageVersion(lib)))
  }
  cli_end(libs_to_print)
}

remove_slash_if_added <- function(path) {
  if(endsWith(path, "/")){
    return(gsub("/$", "", path))
  } else {
    return(path)
  }
}

cli_alert_success("Loaded Armadillo/DataSHIELD libraries:")
show_version_info(c("MolgenisArmadillo", "DSI", "dsBaseClient", "DSMolgenisArmadillo", "resourcer", "dsMediationClient", "dsMTLClient"))

cli_alert_success("Loaded other libraries:")
show_version_info(c("getPass", "arrow", "httr", "jsonlite", "future"))

cli_alert_info("Trying to read config from '.env'")
readRenviron(".env")

armadillo_url = Sys.getenv("ARMADILLO_URL")
if(armadillo_url == ""){
  cli_alert_warning("You probably did not used one of the '*.env.dist' files.")

  cli_alert_warning("Defaulting to https://armadillo-demo.molgenis.net/")

  armadillo_url <- "https://armadillo-demo.molgenis.net/"
} else {
    cli_alert_info(paste0("ARMADILLO_URL from '.env' file: ", armadillo_url))
}

interactive = TRUE
if (Sys.getenv("INTERACTIVE") == 'N') {
  interactive = FALSE
}

armadillo_url <- add_slash_if_not_added(armadillo_url)

if(url.exists(armadillo_url)) {
  cli_alert_success(sprintf("URL [%s] exists", armadillo_url))
  if (!startsWith(armadillo_url, "http")) {
    if (startsWith(armadillo_url, "localhost")) {
        armadillo_url <- paste0("http://", armadillo_url)
    } else {
        armadillo_url <- paste0("https://", armadillo_url)
    }
  }
} else {
  msg <- sprintf("URL [%s] doesn't exist", armadillo_url)
  exit_test(msg)
}

as_docker_container <- FALSE
if("Y" == Sys.getenv("AS_DOCKER_CONTAINER", "N")) {
  as_docker_container <- TRUE
}
cli_alert_info(sprintf("Running in docker container %d", as.integer(as_docker_container)))

service_location = remove_slash_if_added(Sys.getenv("GIT_CLONE_PATH"))
if(service_location == ""){
  cli_alert_warning("Git clone path not set, attempting to set git clone root through normalized path")
  cli_alert_warning("This is assuming you run `Rscript release-test.R` in the same directory as the release-test.R script!")

  service_location <- dirname(dirname(normalizePath(".")))
} else {
   cli_alert_info(paste0("GIT_CHECKOUT_DIR from '.env' file: ", service_location))
}

if(!dir.exists(file.path(service_location, "armadillo"))) {
  exit_test("Service location is not in armadillo clone root.")
}

test_file_path <- remove_slash_if_added(Sys.getenv("TEST_FILE_PATH"))
if(test_file_path == ""){
  cli_alert_warning("Test file path not set, checking for 'testing' folder in data directory")
  testing_path = file.path(service_location, "data", "testing")
  if(!dir.exists(testing_path)){
    cli_alert_info(paste0("Testing directory: ", testing_path, " not found, creating."))
    dir.create(testing_path)
  }
  test_file_path <- testing_path
} else {
  cli_alert_info(paste0("TEST_FILE_PATH from '.env file: ", test_file_path))
  test_file_path <- test_file_path
}

admin_pwd = Sys.getenv("ADMIN_PASSWORD")
if(admin_pwd == ""){
  cli_alert_danger("Admin password not set in .env file, disabling admin mode.")
} else {
  cli_alert_info("ADMIN_PASSWORD from '.env' file")
}

user = Sys.getenv("OIDC_EMAIL")

if(user == "" && admin_pwd == "") {
  exit_test("User and admin password are both not set!")
}

if(user == "") {
  cli_alert_danger("User not set in .env config!")
  cli_alert_info("Enabling admin mode")
  ADMIN_MODE <- TRUE
} else {
  ADMIN_MODE <- FALSE
  cli_alert_info(paste0("USER from '.env. file: ", user))
}
