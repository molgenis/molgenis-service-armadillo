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
