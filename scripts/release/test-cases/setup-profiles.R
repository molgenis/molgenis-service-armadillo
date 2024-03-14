generate_project_port <- function(current_project_ports) {
  starting_port <- 6312
  while (starting_port %in% current_project_ports) {
    starting_port = starting_port + 1
  }
  return(starting_port)
}

obtain_existing_profile_information <- function(key, auth_type) {
  responses <- get_from_api_with_header('ds-profiles', key, auth_type)
  response_df <- data.frame(matrix(ncol=5,nrow=0, dimnames=list(NULL, c("name", "container", "port", "seed", "online"))))
  for (response in responses) {
    if("datashield.seed" %in% names(response$options)) {
      datashield_seed <- response$options$datashield.seed
    } else {
      datashield_seed <- NA
    }

    response_df[nrow(response_df) + 1,] = c(response$name, response$image, response$port, datashield_seed, response$container$status)
  }
  return(response_df)
}

return_list_without_empty <- function(to_empty_list) {
  return(to_empty_list[to_empty_list != ''])
}

create_profile <- function(profile_name, key, auth_type) {
  if (profile_name %in% profile_defaults$name) {
    cli_alert_info(sprintf("Creating profile: %s", profile_name))
    profile_default <- profile_defaults[profile_defaults$name == profile_name,]
    current_profiles <- obtain_existing_profile_information(key, auth_type)
    new_profile_seed <- generate_random_project_seed(current_profiles$seed)
    whitelist <- as.list(stri_split_fixed(paste("dsBase", profile_default$whitelist, sep = ","), ",")[[1]])
    blacklist <- as.list(stri_split_fixed(profile_default$blacklist, ",")[[1]])
    port <- profile_default$port
    if (port == "") {
      port <- generate_project_port(current_profiles$port)
    }
    args <- list(
      name = profile_name,
      image = profile_default$container,
      host = "localhost",
      port = port,
      packageWhitelist = return_list_without_empty(whitelist),
      functionBlacklist = return_list_without_empty(blacklist),
      options = list(datashield.seed = new_profile_seed)
    )
    response <- put_to_api('ds-profiles', key, auth_type, body_args = args)
    if (response$status_code == 204) {
      cli_alert_success(sprintf("Profile %s successfully created.", profile_name))
      start_profile(profile_name, key, auth_type)
    } else {
      exit_test(sprintf("Unable to create profile: %s , errored %s", profile_name, response$status_code))
    }
  } else {
    exit_test(sprintf("Unable to create profile: %s , unknown profile", profile_name))
  }
}

generate_random_project_seed <- function(current_project_seeds) {
  random_seed <- round(runif(1, min = 100000000, max=999999999))
  if (!random_seed %in% current_project_seeds) {
    return(random_seed)
  } else {
    generate_random_project_seed(current_project_seeds)
  }
}

create_profile_if_not_available <- function(profile_name, available_profiles, key, auth_type) {
  if (!profile_name %in% available_profiles) {
    cli_alert_info(sprintf("Unable to locate profile %s, attempting to create.", profile_name))
    create_profile(profile_name, key, auth_type)
  }
  start_profile_if_not_running(profile_name, key, auth_type)
}

start_profile_if_not_running <- function(profile_name, key, auth_type) {
  response <- get_from_api_with_header(paste0("ds-profiles/", profile_name), key, auth_type)
  if (!response$container$status == "RUNNING") {
    cli_alert_info(sprintf("Detected profile %s not running", profile_name))
    start_profile(profile_name, key, auth_type)
  }
}

start_profile <- function(profile_name, key, auth_type) {
  auth_header <- get_auth_header(auth_type, key)
  cli_alert_info(sprintf("Attempting to start profile: %s", profile_name))
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


setup_profiles <- function(token, auth_type, url, as_docker_container, skip_tests, profile, user, interactive) {
  test_name <- "setup-profiles"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }

  cat("\nAvailable profiles: \n")
  profiles <- get_from_api_with_header("profiles", token, auth_type, url)

  cli_alert_info("Checking if profile is prepared for all tests")

  if (!as_docker_container) {
    create_profile_if_not_available(profile, profiles$available, token, auth_type)
  }
  profile_info <- get_from_api_with_header(paste0("ds-profiles/", profile), token, auth_type, url, user)
  if (!as_docker_container) {
    start_profile_if_not_running("default", token, auth_type)
  }
  seed <- unlist(profile_info$options$datashield.seed)
  whitelist <- unlist(profile_info$packageWhitelist)
  if (is.null(seed)) {
    cli_alert_warning(sprintf("Seed of profile [%s] is NULL, please set it in UI profile tab and restart the profile", profile))
    wait_for_input(interactive)
  }
  if (!"resourcer" %in% whitelist) {
    cli_alert_warning(sprintf("Whitelist of profile [%s] does not contain resourcer, please add it and restart the profile", profile))
    wait_for_input(interactive)
  }

  if (!is.null(seed) && "resourcer" %in% whitelist) {
    cli_alert_success(sprintf("%s passed!", test_name))
  }
  return(profile_info)
}
