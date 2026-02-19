generate_project_port <- function(current_project_ports) {
  starting_port <- 6312
  while (starting_port %in% current_project_ports) {
    starting_port <- starting_port + 1
  }
  return(starting_port)
}

obtain_existing_profile_information <- function() {
  responses <- get_from_api_with_header("ds-profiles", release_env$token, release_env$auth_type, release_env$armadillo_url, release_env$user)
  response_df <- data.frame(matrix(ncol = 5, nrow = 0, dimnames = list(NULL, c("name", "container", "port", "seed", "online"))))
  for (response in responses) {
    if ("datashield.seed" %in% names(response$options)) {
      datashield_seed <- response$options$datashield.seed
    } else {
      datashield_seed <- NA
    }

    response_df[nrow(response_df) + 1, ] <- c(response$name, response$image, response$port, datashield_seed, response$container$status)
  }
  return(response_df)
}

return_list_without_empty <- function(to_empty_list) {
  return(to_empty_list[to_empty_list != ""])
}

create_profile <- function(profile_name) {
  if (profile_name %in% release_env$profile_defaults$name) {
    cli_progress_step(sprintf("Creating profile: %s", profile_name))
    profile_default <- release_env$profile_defaults[release_env$profile_defaults$name == profile_name, ]
    current_profiles <- obtain_existing_profile_information()
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
    response <- put_to_api("ds-profiles", release_env$token, release_env$auth_type, body_args = args, url = release_env$armadillo_url)
    if (response$status_code == 204) {
      cli_progress_done()
      start_profile(profile_name)
    } else {
      exit_test(sprintf("Unable to create profile: %s , errored %s", profile_name, response$status_code))
    }
  } else {
    exit_test(sprintf("Unable to create profile: %s , unknown profile", profile_name))
  }
}

generate_random_project_seed <- function(current_project_seeds) {
  random_seed <- round(runif(1, min = 100000000, max = 999999999))
  if (!random_seed %in% current_project_seeds) {
    return(random_seed)
  } else {
    generate_random_project_seed(current_project_seeds)
  }
}

create_profile_if_not_available <- function(profile_name, available_profiles) {
  if (!profile_name %in% available_profiles) {
    cli_alert_info(sprintf("Unable to locate profile %s, attempting to create.", profile_name))
    create_profile(profile_name)
  }
  start_profile_if_not_running(profile_name)
}

start_profile_if_not_running <- function(profile_name) {
  response <- get_from_api_with_header(paste0("ds-profiles/", profile_name), release_env$token, release_env$auth_type, release_env$armadillo_url, release_env$user)
  if (!response$container$status == "RUNNING") {
    cli_alert_info(sprintf("Detected profile %s not running", profile_name))
    start_profile(profile_name)
  }
}

start_profile <- function(profile_name) {
  auth_header <- get_auth_header(release_env$auth_type, release_env$token)
  cli_progress_step(sprintf("Starting profile: %s", profile_name))
  response <- POST(
    sprintf("%sds-profiles/%s/start", release_env$armadillo_url, profile_name),
    config = c(httr::add_headers(auth_header))
  )
  if (response$status_code == 204) {
    cli_progress_done()
  } else if (response$status_code == 409) {
    cli_progress_done()
    cli_alert_info(sprintf("Profile %s already running", profile_name))
  } else {
    cli_progress_done(result = "failed")
    exit_test(sprintf("Unable to start profile %s, error code: %s", profile_name, response$status_code))
  }
}


close_connections <- function() {
  if (is.null(release_env$conns)) {
    cli_alert_info("No open connections")
    return()
  }
  cli_progress_step("Closing DataSHIELD connections")
  datashield.logout(release_env$conns)
  cli_progress_done()
}

restore_admin_permissions <- function() {
  if (release_env$ADMIN_MODE) {
    cli_alert_info("Running as admin, no permissions to restore")
    return()
  }
  if (!isTRUE(release_env$admin_demoted)) {
    cli_alert_info("Admin was not demoted, no permissions to restore")
    return()
  }
  cli_progress_step("Restoring admin permissions")
  set_user(TRUE, list())
  cli_progress_done()
}

delete_created_projects <- function() {
  projects <- unique(release_env$created_projects)
  if (length(projects) == 0) {
    cli_alert_info("No projects to delete")
    return()
  }
  for (project in projects) {
    tryCatch({
      suppressMessages(armadillo.delete_project(project))
      cli_alert_success(sprintf("Deleted project: %s", project))
    }, error = function(e) {
      cli_alert_warning(sprintf("Failed to delete project %s: %s", project, e$message))
    })
  }
}

teardown <- function() {
  tryCatch(close_connections(), error = function(e) {
    cli_alert_warning(sprintf("Failed to close connections: %s", e$message))
  })

  tryCatch(restore_admin_permissions(), error = function(e) {
    cli_alert_warning(sprintf("Failed to restore admin permissions: %s", e$message))
  })

  if(!release_env$debug) {
    tryCatch(delete_created_projects(), error = function(e) {
    cli_alert_warning(sprintf("Failed to delete projects: %s", e$message))
    })
  } else {
    cli_alert_warning("Running in debug mode - project deletion skipped")
  }

  cli_alert_success("Teardown complete")
}

setup_profiles <- function() {
  test_name <- "setup-profiles"
  if (should_skip_test(test_name)) {
    return()
  }

  if (!release_env$as_docker_container) {
    create_profile_if_not_available(release_env$current_profile, release_env$available_profiles)
  }
  profile_info <- get_from_api_with_header(paste0("ds-profiles/", release_env$current_profile), release_env$token, release_env$auth_type, release_env$armadillo_url, release_env$user)
  if (!release_env$as_docker_container) {
    start_profile_if_not_running("default")
  }
  seed <- unlist(profile_info$options$datashield.seed)
  whitelist <- unlist(profile_info$packageWhitelist)
  if (is.null(seed)) {
    cli_alert_warning(sprintf("Seed of profile [%s] is NULL, please set it in UI profile tab and restart the profile", release_env$current_profile))
    wait_for_input(release_env$interactive)
  }
  if (!"resourcer" %in% whitelist) {
    cli_alert_warning(sprintf("Whitelist of profile [%s] does not contain resourcer, please add it and restart the profile", release_env$current_profile))
    wait_for_input(release_env$interactive)
  }

  if (is.null(seed) || !"resourcer" %in% whitelist) {
    exit_test("Profile not properly configured")
  }
  release_env$profile_info <- profile_info
}
