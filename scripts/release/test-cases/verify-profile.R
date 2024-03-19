create_ds_connection <- function(password, token, profile, url, ADMIN_MODE) {
  cli_alert_info("Creating new datashield connection")
  if (ADMIN_MODE) {
    cli_alert_info("Creating connection as admin")
    con <- dsConnect(
      drv = armadillo(),
      name = "armadillo",
      user = "admin",
      password = password,
      url = url,
      profile = profile
    )
  } else {
    cli_alert_info("Creating connection using token")
    con <- dsConnect(
      drv = armadillo(),
      name = "armadillo",
      token = token,
      url = url,
      profile = profile
    )
  }
  return(con)
}

verify_specific_profile <- function(password, token, url, profile, ADMIN_MODE) {
  cli_alert_info("Verify connecting to specified profile works")
  con <- create_ds_connection(password = password, token = token, url = url, profile = profile, ADMIN_MODE)
  if (con@name == "armadillo") {
    cli_alert_success("Succesfully connected")
  } else {
    exit_test("Connection to specific profile failed")
  }
  dsDisconnect(con)
}

verify_no_profile_specified <- function(password, token, url, ADMIN_MODE) {
  cli_alert_info("Verify if default profile works without specifying profile")
  con <- create_ds_connection(password = password, token = token, url = url, profile = "", ADMIN_MODE)
  if (con@name == "armadillo") {
    cli_alert_success("Succesfully connected")
  } else {
    cli_alert_danger("Connection failed")
  }
  dsDisconnect(con)
}

verify_default_profile <- function(password, token, url, ADMIN_MODE) {
  cli_alert_info("Verify if default profile works when specifying profile")
  con <- create_ds_connection(password = password, token = token, url = url, profile = "default", ADMIN_MODE)
  if (con@name == "armadillo") {
    cli_alert_success("Succesfully connected")
  } else {
    cli_alert_danger("Connection failed")
  }
  dsDisconnect(con)
}

verify_profiles <- function(token, url, profile, ADMIN_MODE, admin_pwd, skip_tests) {
  test_name <- "verify-profile"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }

  verify_specific_profile(admin_pwd, token, url, profile, ADMIN_MODE)
  verify_no_profile_specified(admin_pwd, token, url, ADMIN_MODE)
  verify_default_profile(admin_pwd, token, url, ADMIN_MODE)
  cli_alert_success(sprintf("%s passed!", test_name))
}
