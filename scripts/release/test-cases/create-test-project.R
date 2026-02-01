create_test_project <- function() {
  test_name <- "create-test-project"

  test_that("create test project", {
    do_skip_test(test_name)
    cli_alert_info(sprintf("Creating project [%s]", release_env$project1))
    armadillo.create_project(release_env$project1)
    cli_alert_info(sprintf("Checking if project [%s] exists", release_env$project1))
    expect_true(release_env$project1 %in% armadillo.list_projects())
  })
}
