set_admin_or_get_token <- function() {
  test_name <- "set-admin-mode"
  if (should_skip_test(test_name)) {
    return()
  }

  if (release_env$ADMIN_MODE) {
    release_env$token <- release_env$admin_pwd
  } else {
    cli_alert_info("Obtaining TOKEN from '.env.")
    token <- Sys.getenv("TOKEN")
    if (token == "") {
      cli_alert_warning("TOKEN not set, obtaining from armadillo.")
      token <- armadillo.get_token(release_env$armadillo_url)
    }
    release_env$token <- token
  }
  cli_alert_success(sprintf("%s passed!", test_name))
}
