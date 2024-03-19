set_researcher_access <- function(url, interactive, required_projects, user, admin_pwd, update_auto, skip_tests) {
  test_name <- "set_researcher_access"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }

  if (update_auto == "y") {
    if (interactive) {
      cat("\nDo you want to remove admin from OIDC user automatically? (y/n) ")
      update_auto <- readLines("stdin", n = 1)
    }
    set_user(user, admin_pwd, F, required_projects, url)
    if (update_auto != "y") {
      cat("\nGo to the Users tab")
      cat(sprintf("\nAdd [%s]' and [%s] to the project column for your account", unlist(required_projects)))
      cat("\nRevoke your admin permisions\n")
      cli_alert_warning("Make sure you either have the basic auth admin password or someone available to give you back your permissions")
      wait_for_input(interactive)
    }
  }
  cli_alert_success(sprintf("%s passed!", test_name))
}
