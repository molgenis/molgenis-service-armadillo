create_test_project <- function() {
  test_name <- "create-test-project"
  if (do_skip_test(test_name)) {
    return()
  }

  cli_alert_info(sprintf("Creating project [%s]", release_env$project1))
  armadillo.create_project(release_env$project1)
  cli_alert_info(sprintf("Checking if project [%s] exists", release_env$project1))
  check_cohort_exists(release_env$project1)
  cli_alert_success(sprintf("%s passed!", test_name))
}
