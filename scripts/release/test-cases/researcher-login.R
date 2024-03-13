create_dsi_builder <- function(server = "armadillo", url, profile, password = "", token = "", table = "", resource = "", ADMIN_MODE) {
  cli_alert_info("Creating new datashield login builder")
  builder <- DSI::newDSLoginBuilder()
  if (ADMIN_MODE) {
    cli_alert_info("Appending information as admin")
    builder$append(
      server = server,
      url = url,
      profile = profile,
      table = table,
      driver = "ArmadilloDriver",
      user = "admin",
      password = password,
      resource = resource
    )
  } else {
    cli_alert_info("Appending information using token")
    builder$append(
      server = server,
      url = url,
      profile = profile,
      table = table,
      driver = "ArmadilloDriver",
      token = token,
      resource = resource
    )
  }
  cli_alert_info("Appending information to login builder")
  return(builder$build())
}

researcher_login <- function(url, profile, admin_pwd, token, table, project, object, variables, ADMIN_MODE, skip_tests) {
    test_name <- "researcher_login"
    if(do_skip_test(test_name, skip_tests)) {return()}

    logindata <- create_dsi_builder(url = url, profile = profile, password = admin_pwd, token = token, table = sprintf("%s/%s", project, table), ADMIN_MODE = ADMIN_MODE)
    cli_alert_info(sprintf("Login with profile [%s] and table: [%s/2_1-core-1_0/nonrep]", profile, project))
    conns <- datashield.login(logins = logindata, symbol = object, variables = variables, assign = TRUE)
    cli_alert_success("Logged in")
    return(conns)
    }
