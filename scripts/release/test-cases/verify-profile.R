create_ds_connection <- function(password = "", token = "", profile = "", url) {
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

verify_profile <- function(password, token, url, profile){

    con <- create_ds_connection(password = admin_pwd, token = token, url = armadillo_url, profile = profile)
    if (con@name == "armadillo") {
      cli_alert_success("Succesfully connected")
    } else {
      # FIXME: should we exit?
      cli_alert_danger("Connection failed")
    }
    dsDisconnect(con)
}
