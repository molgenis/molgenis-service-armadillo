create_dsi_builder <- function(server = "armadillo", table = "", resource = "") {
  cli_alert_info("Creating new datashield login builder")
  builder <- DSI::newDSLoginBuilder()
  if (release_env$ADMIN_MODE) {
    cli_alert_info("Appending information as admin")
    builder$append(
      server = server,
      url = release_env$armadillo_url,
      profile = release_env$current_profile,
      table = table,
      driver = "ArmadilloDriver",
      user = "admin",
      password = release_env$admin_pwd,
      resource = resource
    )
  } else {
    cli_alert_info("Appending information using token")
    builder$append(
      server = server,
      url = release_env$armadillo_url,
      profile = release_env$current_profile,
      table = table,
      driver = "ArmadilloDriver",
      token = release_env$token,
      resource = resource
    )
  }
  cli_alert_info("Appending information to login builder")
  return(builder$build())
}

researcher_login <- function(table, object, variables) {
  test_name <- "researcher_login"
  if (do_skip_test(test_name)) {
    return()
  }

  full_table <- sprintf("%s/%s", release_env$project1, table)
  logindata <- create_dsi_builder(table = full_table)
  cli_alert_info(sprintf("Login with profile [%s] and table: [%s/%s]", release_env$current_profile, release_env$project1, table))
  release_env$conns <- datashield.login(logins = logindata, symbol = object, variables = variables, assign = TRUE)
  cli_alert_success(sprintf("%s passed!", test_name))
}
