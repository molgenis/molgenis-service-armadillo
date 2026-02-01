dm_login <- function() {
  test_name <- "dm_login"
  if (do_skip_test(test_name)) {
    return()
  }

  cli_alert_info(sprintf("Login to %s", release_env$armadillo_url))
  if (release_env$ADMIN_MODE) {
    armadillo.login_basic(release_env$armadillo_url, "admin", release_env$admin_pwd)
  } else {
    armadillo.login(release_env$armadillo_url)
  }
  cli_alert_success(sprintf("%s passed!", test_name))
}
