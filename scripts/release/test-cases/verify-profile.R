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

verify_profiles <- function() {
  test_name <- "verify-profile"

  test_that("connect to specified profile", {
    do_skip_test(test_name)
    con <- create_ds_connection(profile = release_env$current_profile)
    expect_equal(con@name, "armadillo")
    dsDisconnect(con)
  })

  test_that("connect without specifying profile", {
    do_skip_test(test_name)
    con <- create_ds_connection(profile = "")
    expect_equal(con@name, "armadillo")
    dsDisconnect(con)
  })

  test_that("connect to default profile", {
    do_skip_test(test_name)
    con <- create_ds_connection(profile = "default")
    expect_equal(con@name, "armadillo")
    dsDisconnect(con)
  })
}
