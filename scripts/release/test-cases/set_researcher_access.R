set_researcher_access <- function() {
  test_name <- "set_researcher_access"

  test_that("set researcher access", {
    do_skip_test(test_name)

    if (release_env$update_auto == "y") {
      if (release_env$interactive) {
        cat("\nDo you want to remove admin from OIDC user automatically? (y/n) ")
        update_auto <- readLines("stdin", n = 1)
      }
      set_user(F, list(release_env$project1))
      if (release_env$update_auto != "y") {
        cat("\nGo to the Users tab")
        cat(sprintf("\nAdd [%s]' and [%s] to the project column for your account", unlist(list(release_env$project1))))
        cat("\nRevoke your admin permisions\n")
        cli_alert_warning("Make sure you either have the basic auth admin password or someone available to give you back your permissions")
        wait_for_input(release_env$interactive)
      }
    }
    succeed()
  })
}
