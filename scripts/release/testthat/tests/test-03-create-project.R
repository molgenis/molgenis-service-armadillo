# Setup
test_name <- "create-test-project"

test_that("create test project", {
  do_skip_test(test_name)
  armadillo.create_project(release_env$project1)
  expect_true(release_env$project1 %in% armadillo.list_projects())
})
