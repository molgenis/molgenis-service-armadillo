dm_login <- function(url, ADMIN_MODE, admin_pwd, skip_tests) {
  test_name <- "dm_login"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }

  cli_alert_info(sprintf("Login to %s", url))
  if (test_config$ADMIN_MODE) {
    armadillo.login_basic(url, "admin", test_config$admin_pwd)
  } else {
    armadillo.login(url)
  }
  cli_alert_success(sprintf("%s passed!", test_name))
}
