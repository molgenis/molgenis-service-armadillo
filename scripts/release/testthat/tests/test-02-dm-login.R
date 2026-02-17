# Setup
test_name <- "dm-login"

test_that("data manager login", {
  do_skip_test(test_name)
  # Login happens in setup.R, verify by listing projects
  expect_no_error(armadillo.list_projects())
})
