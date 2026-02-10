set_admin_or_get_token <- function() {
  test_name <- "set-admin-mode"
  if (should_skip_test(test_name)) {
    return()
  }

  if (release_env$ADMIN_MODE) {
    cli_alert_success("Using admin password")
    release_env$token <- release_env$admin_pwd
  } else {
    token <- Sys.getenv("TOKEN")
    if (token == "") {
      cli_progress_step("Fetching token")
      suppressMessages(token <- armadillo.get_credentials(release_env$armadillo_url)@access_token)
      cli_progress_done()
    } else {
      cli_alert_success("Using token from .env")
    }
    release_env$token <- token
  }
}
