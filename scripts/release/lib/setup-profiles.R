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

  user_skips <- release_env$skip_tests[release_env$skip_tests != ""]
  not_installed <- setdiff(all_ds_tests, ds_packages)

  cat("\n")
  cli_alert_info(sprintf("Image: %s", image))
  cli_alert_info(sprintf("Resource support: %s", if (has_resourcer) "Yes" else "No"))
  cli_alert_info(sprintf("DS packages (%d): %s", length(ds_packages), paste(ds_packages, collapse = ", ")))
  cli_alert_info(sprintf("Skipped by user: %s",
    if (length(user_skips) == 0) "None" else paste(user_skips, collapse = ", ")))
  cli_alert_info(sprintf("Skipped (package not available): %s",
    if (length(not_installed) == 0) "None" else paste(not_installed, collapse = ", ")))
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
