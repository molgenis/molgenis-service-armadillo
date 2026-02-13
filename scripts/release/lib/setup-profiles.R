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


# Fetch all installed R packages from the Armadillo API for the current profile.
# Uses an httr handle to maintain a session: first selects the profile (so the
# session-scoped Commands bean connects to the right Rock server), then queries
# /packages within the same session.
get_installed_packages <- function() {
  auth_header <- get_auth_header(release_env$auth_type, release_env$token)
  base_url <- release_env$armadillo_url
  h <- httr::handle(base_url)

  # Select the current profile within this session
  select_response <- POST(
    paste0(base_url, "select-profile"),
    body = release_env$current_profile,
    encode = "raw",
    config = c(httr::content_type("text/plain"), httr::add_headers(auth_header)),
    handle = h
  )
  if (select_response$status_code != 204) {
    cli_alert_warning(sprintf(
      "Failed to select profile '%s' (status %s), package detection may be incomplete",
      release_env$current_profile, select_response$status_code
    ))
  }

  # Get packages for the selected profile
  response <- GET(
    paste0(base_url, "packages"),
    config = c(httr::add_headers(auth_header)),
    handle = h
  )
  if (response$status_code != 200) {
    cli_alert_warning("Failed to fetch packages from API")
    return(list())
  }

  content(response)
}

# Filter to packages that declare DataShield methods
extract_ds_package_names <- function(packages) {
  ds_packages <- character(0)
  for (pkg in packages) {
    if (!is.null(pkg$assignMethods) || !is.null(pkg$aggregateMethods)) {
      ds_packages <- c(ds_packages, pkg$name)
    }
  }
  ds_packages
}

# Update the profile whitelist via the API
update_profile_whitelist <- function(new_whitelist) {
  args <- list(
    name = release_env$current_profile,
    image = release_env$profile_info$image,
    host = release_env$profile_info$host,
    port = release_env$profile_info$port,
    packageWhitelist = as.list(new_whitelist),
    functionBlacklist = as.list(unlist(release_env$profile_info$functionBlacklist)),
    options = release_env$profile_info$options
  )
  put_to_api(
    "ds-profiles", release_env$token, release_env$auth_type,
    body_args = args, url = release_env$armadillo_url
  )
}

# Main entry point: detect installed DS packages and whitelist them
detect_and_whitelist_packages <- function() {
  cli_progress_step("Detecting installed DataShield packages")
  packages <- get_installed_packages()
  ds_packages <- extract_ds_package_names(packages)
  cli_progress_done()

  current_whitelist <- unlist(release_env$profile_info$packageWhitelist)
  missing <- setdiff(ds_packages, current_whitelist)

  if (length(missing) > 0) {
    cli_alert_info(sprintf("Auto-whitelisting: %s", paste(missing, collapse = ", ")))
    response <- update_profile_whitelist(union(current_whitelist, ds_packages))
    if (response$status_code == 204) {
      cli_alert_success("Profile whitelist updated")
      # No profile restart needed: the PUT already flushes profile-scoped beans
      # (including the DS environment cache), so the new whitelist takes effect
      # on the next request. Restarting would pull the Docker image and recreate
      # the container, risking server-side package version changes.
      release_env$profile_info <- get_from_api_with_header(
        paste0("ds-profiles/", release_env$current_profile),
        release_env$token, release_env$auth_type,
        release_env$armadillo_url, release_env$user
      )
    } else {
      cli_alert_warning(sprintf("Failed to update whitelist (status %s)", response$status_code))
    }
  }

  release_env$installed_ds_packages <- ds_packages
}

# Display profile setup summary (shown under the existing "Testing profile: X" h2)
show_profile_info <- function() {
  image <- release_env$profile_info$image
  ds_packages <- release_env$installed_ds_packages
  has_resourcer <- "resourcer" %in% ds_packages

  # All DS test package names (must match test_name values in test files)
  all_ds_tests <- c("dsBase", "dsMediation", "dsSurvival", "dsMTLBase", "dsExposome", "dsOmics", "dsTidyverse")

  # Tests that will skip: user-requested (SKIP_TESTS) + packages not installed
  user_skips <- release_env$skip_tests[release_env$skip_tests != ""]
  missing_skips <- setdiff(all_ds_tests, ds_packages)
  all_skips <- unique(c(user_skips, missing_skips))
  skipping <- if (length(all_skips) == 0) "None" else paste(all_skips, collapse = ", ")

  cat("\n")
  cli_alert_info(sprintf("Image: %s", image))
  cli_alert_info(sprintf("Resource support: %s", if (has_resourcer) "Yes" else "No"))
  cli_alert_info(sprintf("DS packages (%d): %s", length(ds_packages), paste(ds_packages, collapse = ", ")))
  cli_alert_info(sprintf("Skipping: %s", skipping))
  cat("\n")
}

setup_profiles <- function() {
  test_name <- "setup-profiles"
  if (should_skip_test(test_name)) {
    return()
  }

  profile_info <- get_from_api_with_header(paste0("ds-profiles/", release_env$current_profile), release_env$token, release_env$auth_type, release_env$armadillo_url, release_env$user)

  # Ensure the profile is running before detecting packages
  if (!identical(profile_info$container$status, "RUNNING")) {
    start_profile(release_env$current_profile)
  }
  seed <- unlist(profile_info$options$datashield.seed)
  if (is.null(seed)) {
    cli_alert_warning(sprintf("Seed of profile [%s] is NULL, please set it in UI profile tab and restart the profile", release_env$current_profile))
    wait_for_input(release_env$interactive)
    exit_test("Profile not properly configured")
  }
  release_env$profile_info <- profile_info
  detect_and_whitelist_packages()
  show_profile_info()
}
