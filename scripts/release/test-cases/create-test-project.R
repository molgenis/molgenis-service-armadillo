create_test_project <- function(target_project_name, skip_tests) {
  test_name <- "create-test-project"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }

  cli_alert_info(sprintf("Creating project [%s]", target_project_name))
  armadillo.create_project(target_project_name)
  cli_alert_info(sprintf("Checking if project [%s] exists", target_project_name))
  check_cohort_exists(target_project_name)
  cli_alert_success(sprintf("%s passed!", test_name))
}
