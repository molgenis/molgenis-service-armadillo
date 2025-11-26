create_ds_connection <- function(password, token, container, url, ADMIN_MODE) {
  cli_alert_info("Creating new datashield connection")
  if (ADMIN_MODE) {
    cli_alert_info("Creating connection as admin")
    con <- dsConnect(
      drv = armadillo(),
      name = "armadillo",
      user = "admin",
      password = password,
      url = url,
      profile = container
    )
  } else {
    cli_alert_info("Creating connection using token")
    con <- dsConnect(
      drv = armadillo(),
      name = "armadillo",
      token = token,
      url = url,
      profile = container
    )
  }
  return(con)
}

verify_specific_container <- function(password, token, url, container, ADMIN_MODE) {
  cli_alert_info("Verify connecting to specified container works")
  con <- create_ds_connection(password = password, token = token, url = url, container = container, ADMIN_MODE)
  if (con@name == "armadillo") {
    cli_alert_success("Succesfully connected")
  } else {
    exit_test("Connection to specific container failed")
  }
  dsDisconnect(con)
}

verify_no_container_specified <- function(password, token, url, ADMIN_MODE) {
  cli_alert_info("Verify if default container works without specifying container")
  con <- create_ds_connection(password = password, token = token, url = url, container = "", ADMIN_MODE)
  if (con@name == "armadillo") {
    cli_alert_success("Succesfully connected")
  } else {
    cli_alert_danger("Connection failed")
  }
  dsDisconnect(con)
}

verify_default_container <- function(password, token, url, ADMIN_MODE) {
  cli_alert_info("Verify if default container works")
  con <- create_ds_connection(password = password, token = token, url = url, container = "default", ADMIN_MODE)
  if (con@name == "armadillo") {
    cli_alert_success("Succesfully connected")
  } else {
    cli_alert_danger("Connection failed")
  }
  dsDisconnect(con)
}

verify_containers <- function(token, url, container, ADMIN_MODE, admin_pwd, skip_tests) {
  test_name <- "verify-container"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }

  verify_specific_container(admin_pwd, token, url, container, ADMIN_MODE)
  verify_no_container_specified(admin_pwd, token, url, ADMIN_MODE)
  verify_default_container(admin_pwd, token, url, ADMIN_MODE)
  cli_alert_success(sprintf("%s passed!", test_name))
}
