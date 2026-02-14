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

  # Use an explicit httr handle so the JSESSIONID cookie from the first request
  # carries through to subsequent requests in the same session. This is needed
  # because select-profile is session-scoped.
  h <- httr::handle(base_url)
  auth_config <- httr::add_headers(auth_header)

  # Select the current profile within this session
  select_response <- POST(
    paste0(base_url, "select-profile"),
    body = release_env$current_profile,
    encode = "raw",
    config = c(httr::content_type("text/plain"), auth_config),
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
    config = c(auth_config),
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

# Re-fetch profile info from the API and store in release_env
refresh_profile_info <- function() {
  release_env$profile_info <- get_from_api_with_header(
    paste0("ds-profiles/", release_env$current_profile),
    release_env$token, release_env$auth_type,
    release_env$armadillo_url, release_env$user
  )
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
  # The /packages endpoint creates a Rock connection that loads all whitelisted
  # packages. If the whitelist is empty, the server sends library("") to R
  # which fails with "invalid package name". Initialise with dsBase (always
  # present) so the connection can be created.
  initialise_empty_whitelist()

  ds_packages <- detect_installed_ds_packages()

  update_whitelist_if_needed(ds_packages)

  release_env$installed_ds_packages <- ds_packages
}

# If the whitelist is empty, set it to dsBase so /packages can create a connection
initialise_empty_whitelist <- function() {
  current_whitelist <- unlist(release_env$profile_info$packageWhitelist)
  if (length(current_whitelist) > 0) return()

  cli_alert_info("Whitelist empty, initialising with dsBase")
  response <- update_profile_whitelist(c("dsBase"))
  if (response$status_code == 204) {
    refresh_profile_info()
  }
}

# Query the API for installed DS packages, with retries for slow profile startup
detect_installed_ds_packages <- function() {
  cli_progress_step("Detecting installed DataShield packages")
  max_retries <- 5
  ds_packages <- character(0)
  for (attempt in seq_len(max_retries)) {
    packages <- get_installed_packages()
    ds_packages <- extract_ds_package_names(packages)
    if (length(ds_packages) > 0) break
    if (attempt < max_retries) {
      cli_progress_step(sprintf("Profile not ready, retrying (%d/%d)", attempt, max_retries))
      Sys.sleep(5)
    }
  }
  if (length(ds_packages) == 0) {
    cli_progress_done(result = "failed")
    exit_test(sprintf("No DataShield packages detected for profile '%s' after %d attempts",
      release_env$current_profile, max_retries))
  }
  cli_progress_done()
  ds_packages
}

# Add any missing DS packages to the profile whitelist
update_whitelist_if_needed <- function(ds_packages) {
  current_whitelist <- unlist(release_env$profile_info$packageWhitelist)
  missing <- setdiff(ds_packages, current_whitelist)
  if (length(missing) == 0) return()

  cli_alert_info(sprintf("Auto-whitelisting: %s", paste(missing, collapse = ", ")))
  response <- update_profile_whitelist(union(current_whitelist, ds_packages))
  if (response$status_code == 204) {
    cli_alert_success("Profile whitelist updated")
    refresh_profile_info()
  } else {
    cli_alert_warning(sprintf("Failed to update whitelist (status %s)", response$status_code))
  }
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

  refresh_profile_info()

  # Ensure the profile is running before detecting packages
  if (!identical(release_env$profile_info$container$status, "RUNNING")) {
    start_profile(release_env$current_profile)
  }
  seed <- unlist(release_env$profile_info$options$datashield.seed)
  if (is.null(seed)) {
    cli_alert_warning(sprintf("Seed of profile [%s] is NULL, please set it in UI profile tab and restart the profile",
      release_env$current_profile))
    wait_for_input(release_env$interactive)
    exit_test("Profile not properly configured")
  }
  detect_and_whitelist_packages()
  show_profile_info()
}
