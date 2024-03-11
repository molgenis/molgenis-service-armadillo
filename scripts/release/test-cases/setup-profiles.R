# get request to armadillo api with an authheader
get_from_api_with_header <- function(endpoint, key, auth_type) {
  auth_header <- get_auth_header(auth_type, key)
  response <- GET(paste0(armadillo_url, endpoint), config = c(httr::add_headers(auth_header)))
  if(response$status_code == 403){
    msg <- sprintf("Permission denied. Is user [%s] admin?", user)
    exit_test(msg)
  } else if(response$status_code != 200) {
    cli_alert_danger(sprintf("Cannot retrieve data from endpoint [%s]", endpoint))
    exit_test(content(response)$message)
  }
  return(content(response))
}

# make authentication header for api calls, basic or bearer based on type
get_auth_header <- function(type, key) {
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

create_bearer_header <- function(token){
  return(paste0("Bearer ", token))
}

print_list <- function(list) {
  vals_to_print <- cli_ul()
  for (i in 1:length(list)) {
    val = list[i]
    cli_li(val)
  }
  cli_end(vals_to_print)
}

create_profile_if_not_available <- function(profile_name, available_profiles, key, auth_type) {
  if (!profile_name %in% available_profiles) {
    cli_alert_info(sprintf("Unable to locate profile %s, attempting to create.", profile_name))
    create_profile(profile_name, key, auth_type)
  }
  start_profile_if_not_running(profile_name, key, auth_type)
}

start_profile_if_not_running <- function(profile_name, key, auth_type) {
  response <- get_from_api_with_header(paste0('ds-profiles/', profile_name), key, auth_type)
  if (!response$container$status == "RUNNING") {
    cli_alert_info(sprintf("Detected profile %s not running", profile_name))
    start_profile(profile_name, key, auth_type)
  }
}

start_profile <- function(profile_name, key, auth_type) {
  auth_header <- get_auth_header(auth_type, key)
  cli_alert_info(sprintf('Attempting to start profile: %s', profile_name))
  response <- POST(
    sprintf("%sds-profiles/%s/start", armadillo_url, profile_name),
    config = c(httr::add_headers(auth_header))
    )
  if (!response$status_code == 204) {
    exit_test(sprintf("Unable to start profile %s, error code: %s", profile_name, response$status_code))
  } else {
    cli_alert_success(sprintf("Successfully started profile: %s", profile_name))
  }
}


setup_profiles <- function(token, auth_type, skip_tests) {
cat("\nAvailable profiles: \n")
profiles <- get_from_api_with_header("profiles", token, auth_type)
print_list(unlist(profiles$available))

profile = Sys.getenv("PROFILE")
if(profile == ""){
  cli_alert_warning("Profile not set, defaulting to xenon.")
  profile <- "xenon"
} else {
  cli_alert_info(paste0("PROFILE from '.env' file: ", profile))
}

cli_alert_info("Checking if profile is prepared for all tests")

if (!as_docker_container) {
  create_profile_if_not_available(profile, profiles$available, token, auth_type)
}
profile_info <- get_from_api_with_header(paste0("ds-profiles/", profile), token, auth_type)
if (!as_docker_container) {
  start_profile_if_not_running("default", token, auth_type)
}
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
