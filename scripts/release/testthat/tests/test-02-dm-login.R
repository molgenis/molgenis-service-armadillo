# Setup
test_name <- "dm_login"

test_that("data manager login", {
  do_skip_test(test_name)
  cli_alert_info(sprintf("Login to %s", release_env$armadillo_url))
  if (release_env$ADMIN_MODE) {
    expect_no_error(armadillo.login_basic(release_env$armadillo_url, "admin", release_env$admin_pwd))
  } else {
    expect_no_error(armadillo.login(release_env$armadillo_url))
  }
})
