generate_project_port <- function(current_project_ports) {
  starting_port <- 6312
  while (starting_port %in% current_project_ports) {
    starting_port <- starting_port + 1
  }
  return(starting_port)
}

obtain_existing_container_information <- function(key, auth_type) {
  responses <- get_from_api_with_header("ds-containers", key, auth_type)
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

create_container <- function(container_name, key, auth_type, container_defaults) {
  if (container_name %in% container_defaults$name) {
    cli_alert_info(sprintf("Creating container: %s", container_name))
    container_default <- container_defaults[container_defaults$name == container_name, ]
    current_containers <- obtain_existing_container_information(key, auth_type)
    new_container_seed <- generate_random_project_seed(current_containers$seed)
    whitelist <- as.list(stri_split_fixed(paste("dsBase", container_default$whitelist, sep = ","), ",")[[1]])
    blacklist <- as.list(stri_split_fixed(container_default$blacklist, ",")[[1]])
    port <- container_default$port
    if (port == "") {
      port <- generate_project_port(current_containers$port)
    }
    args <- list(
      name = container_name,
      image = container_default$container,
      host = "localhost",
      port = port,
      packageWhitelist = return_list_without_empty(whitelist),
      functionBlacklist = return_list_without_empty(blacklist),
      options = list(datashield.seed = new_container_seed)
    )
    response <- put_to_api("ds-containers", key, auth_type, body_args = args)
    if (response$status_code == 204) {
      cli_alert_success(sprintf("container %s successfully created.", container_name))
      start_container(container_name, key, auth_type)
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

create_container_if_not_available <- function(container_name, available_containers, key, auth_type, container_defaults) {
  if (!container_name %in% available_containers) {
    cli_alert_info(sprintf("Unable to locate container %s, attempting to create.", container_name))
    create_container(container_name, key, auth_type, container_defaults)
  }
  start_container_if_not_running(container_name, key, auth_type)
}

start_container_if_not_running <- function(container_name, key, auth_type) {
  response <- get_from_api_with_header(paste0("ds-containers/", container_name), key, auth_type)
  if (!response$container$status == "RUNNING") {
    cli_alert_info(sprintf("Detected container %s not running", container_name))
    start_container(container_name, key, auth_type)
  }
}

start_container <- function(container_name, key, auth_type) {
  auth_header <- get_auth_header(auth_type, key)
  cli_alert_info(sprintf("Attempting to start container: %s", container_name))
  response <- POST(
    sprintf("%sds-containers/%s/start", armadillo_url, container_name),
    config = c(httr::add_headers(auth_header))
  )
  if (!response$status_code == 204) {
    exit_test(sprintf("Unable to start container %s, error code: %s", container_name, response$status_code))
  } else {
    cli_alert_success(sprintf("Successfully started container: %s", container_name))
  }
}


setup_containers <- function(token, auth_type, url, as_docker_container, skip_tests, container, user, interactive, container_defaults) {
  test_name <- "setup-containers"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }

  cat("\nAvailable containers: \n")
  containers <- get_from_api_with_header("containers", token, auth_type, url, user)

  cli_alert_info("Checking if container is prepared for all tests")

  if (!as_docker_container) {
    create_container_if_not_available(container, containers$available, token, auth_type, container_defaults)
  }
  container_info <- get_from_api_with_header(paste0("ds-containers/", container), token, auth_type, url, user)
  if (!as_docker_container) {
    start_container_if_not_running("default", token, auth_type)
  }
  seed <- unlist(container_info$options$datashield.seed)
  whitelist <- unlist(container_info$packageWhitelist)
  if (is.null(seed)) {
    cli_alert_warning(sprintf("Seed of container [%s] is NULL, please set it in UI container tab and restart the container", container))
    wait_for_input(interactive)
  }
  if (!"resourcer" %in% whitelist) {
    cli_alert_warning(sprintf("Whitelist of container [%s] does not contain resourcer, please add it and restart the container", container))
    wait_for_input(interactive)
  }

  if (!is.null(seed) && "resourcer" %in% whitelist) {
    cli_alert_success(sprintf("%s passed!", test_name))
  }
  return(container_info)
}
