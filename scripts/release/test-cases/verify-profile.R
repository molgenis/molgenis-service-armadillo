create_ds_connection <- function(password, token, profile, url) {
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

verify_specific_profile <- function(password, token, url, profile) {
    cli_alert_info("Verify connecting to specified profile works")
    con <- create_ds_connection(password = admin_pwd, token = token, url = url, profile = profile)
    if (con@name == "armadillo") {
      cli_alert_success("Succesfully connected")
    } else {
      # FIXME: should we exit?
      cli_alert_danger("Connection failed")
    }
     dsDisconnect(con)
}

verify_no_profile_specified <- function(password, token, url) {
    cli_alert_info("Verify if default profile works without specifying profile")
    con <- create_ds_connection(password = password, token = token, url = url, profile = "")
    if (con@name == "armadillo") {
      cli_alert_success("Succesfully connected")
    } else {
      cli_alert_danger("Connection failed")
    }
    dsDisconnect(con)
}

verify_default_profile <- function(password, token, url) {
    cli_alert_info("Verify if default profile works when specifying profile")
    con <- create_ds_connection(password = password, token = token, url = url, profile = "default")
    if (con@name == "armadillo") {
      cli_alert_success("Succesfully connected")
    } else {
      cli_alert_danger("Connection failed")
    }
    dsDisconnect(con)
}

verify_profiles <- function(password, token, url, profile) {
    verify_specific_profile(password, token, url, profile)
    verify_no_profile_specified(password, token, url)
    verify_default_profile(password, token, url)
}
