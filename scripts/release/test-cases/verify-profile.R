create_ds_connection <- function(profile = "") {
  cli_alert_info("Creating new datashield connection")
  if (release_env$ADMIN_MODE) {
    cli_alert_info("Creating connection as admin")
    con <- dsConnect(
      drv = armadillo(),
      name = "armadillo",
      user = "admin",
      password = release_env$admin_pwd,
      url = release_env$armadillo_url,
      profile = profile
    )
  } else {
    cli_alert_info("Creating connection using token")
    con <- dsConnect(
      drv = armadillo(),
      name = "armadillo",
      token = release_env$token,
      url = release_env$armadillo_url,
      profile = profile
    )
  }
  return(con)
}

verify_specific_profile <- function() {
  cli_alert_info("Verify connecting to specified profile works")
  con <- create_ds_connection(profile = release_env$current_profile)
  if (con@name == "armadillo") {
    cli_alert_success("Succesfully connected")
  } else {
    exit_test("Connection to specific profile failed")
  }
  dsDisconnect(con)
}

verify_no_profile_specified <- function() {
  cli_alert_info("Verify if default profile works without specifying profile")
  con <- create_ds_connection(profile = "")
  if (con@name == "armadillo") {
    cli_alert_success("Succesfully connected")
  } else {
    cli_alert_danger("Connection failed")
  }
  dsDisconnect(con)
}

verify_default_profile <- function() {
  cli_alert_info("Verify if default profile works when specifying profile")
  con <- create_ds_connection(profile = "default")
  if (con@name == "armadillo") {
    cli_alert_success("Succesfully connected")
  } else {
    cli_alert_danger("Connection failed")
  }
  dsDisconnect(con)
}

verify_profiles <- function() {
  test_name <- "verify-profile"
  if (do_skip_test(test_name)) {
    return()
  }

  verify_specific_profile()
  verify_no_profile_specified()
  verify_default_profile()
  cli_alert_success(sprintf("%s passed!", test_name))
}
