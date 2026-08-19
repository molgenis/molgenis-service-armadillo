generate_project_port <- function(current_project_ports) {
  starting_port <- 6312
  while (starting_port %in% current_project_ports) {
    starting_port <- starting_port + 1
  }
  return(starting_port)
}

obtain_existing_container_information <- function() {
  responses <- get_from_api_with_header("containers", release_env$token, release_env$auth_type, release_env$armadillo_url, release_env$user)
  response_df <- data.frame(matrix(ncol = 5, nrow = 0, dimnames = list(NULL, c("name", "container", "port", "seed", "online"))))
  for (response in responses) {
    ds_options <- response$specificContainerOptions$datashieldROptions
    if ("datashield.seed" %in% names(ds_options)) {
      datashield_seed <- ds_options$datashield.seed
    } else {
      datashield_seed <- NA
    }

    response_df[nrow(response_df) + 1, ] <- c(response$name, response$image, response$port, datashield_seed, response$dockerStatus$status)
  }
  return(response_df)
}

return_list_without_empty <- function(to_empty_list) {
  return(to_empty_list[to_empty_list != ""])
}

create_container <- function(container_name) {
  if (container_name %in% release_env$container_defaults$name) {
    cli_progress_step(sprintf("Creating container: %s", container_name))
    container_default <- release_env$container_defaults[release_env$container_defaults$name == container_name, ]
    current_containers <- obtain_existing_container_information()
    new_container_seed <- generate_random_project_seed(current_containers$seed)
    whitelist <- as.list(stri_split_fixed(paste("dsBase", container_default$whitelist, sep = ","), ",")[[1]])
    blacklist <- as.list(stri_split_fixed(container_default$blacklist, ",")[[1]])
    port <- container_default$port
    if (port == "") {
      port <- generate_project_port(current_containers$port)
    }
    args <- list(
      type = "ds",
      name = container_name,
      image = container_default$container,
      host = "localhost",
      port = port,
      packageWhitelist = return_list_without_empty(whitelist),
      functionBlacklist = return_list_without_empty(blacklist),
      datashieldROptions = list(datashield.seed = new_container_seed)
    )
    response <- put_to_api("containers", release_env$token, release_env$auth_type, body_args = args, url = release_env$armadillo_url)
    if (response$status_code == 204) {
      cli_progress_done()
      start_container(container_name)
    } else {
      exit_test(sprintf("Unable to create container: %s , errored %s", container_name, response$status_code))
    }
  } else {
    exit_test(sprintf("Unable to create container: %s , unknown container", container_name))
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

create_container_if_not_available <- function(container_name, available_containers) {
  if (!container_name %in% available_containers) {
    cli_alert_info(sprintf("Unable to locate container %s, attempting to create.", container_name))
    create_container(container_name)
  }
  start_container_if_not_running(container_name)
}

start_container_if_not_running <- function(container_name) {
  response <- get_from_api_with_header(paste0("containers/", container_name), release_env$token, release_env$auth_type, release_env$armadillo_url, release_env$user)
  if (!response$dockerStatus$status == "RUNNING") {
    cli_alert_info(sprintf("Detected container %s not running", container_name))
    start_container(container_name)
  }
}

start_container <- function(container_name) {
  auth_header <- get_auth_header(release_env$auth_type, release_env$token)
  cli_progress_step(sprintf("Starting container: %s", container_name))
  response <- POST(
    sprintf("%scontainers/%s/start", release_env$armadillo_url, container_name),
    config = c(httr::add_headers(auth_header))
  )
  if (response$status_code == 204) {
    cli_progress_done()
  } else if (response$status_code == 409) {
    cli_progress_done()
    cli_alert_info(sprintf("Container %s already running", container_name))
  } else {
    cli_progress_done(result = "failed")
    exit_test(sprintf("Unable to start container %s, error code: %s", container_name, response$status_code))
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

setup_containers <- function() {
  test_name <- "setup-containers"
  if (should_skip_test(test_name)) {
    return()
  }

  if (!release_env$as_docker_container) {
    create_container_if_not_available(release_env$current_container, release_env$available_containers)
  }
  container_info <- get_from_api_with_header(paste0("containers/", release_env$current_container), release_env$token, release_env$auth_type, release_env$armadillo_url, release_env$user)
  if (!release_env$as_docker_container) {
    start_container_if_not_running("default")
  }
  seed <- unlist(container_info$specificContainerOptions$datashieldROptions$datashield.seed)
  whitelist <- unlist(container_info$specificContainerOptions$packageWhitelist)
  if (is.null(seed)) {
    cli_alert_warning(sprintf("Seed of container [%s] is NULL, please set it in UI container tab and restart the container", release_env$current_container))
    wait_for_input(release_env$interactive)
  }
  if (!"resourcer" %in% whitelist) {
    cli_alert_warning(sprintf("Whitelist of container [%s] does not contain resourcer, please add it and restart the container", release_env$current_container))
    wait_for_input(release_env$interactive)
  }

  if (is.null(seed) || !"resourcer" %in% whitelist) {
    exit_test("Container not properly configured")
  }
  release_env$container_info <- container_info
}