test_that("Admin Authentication", {
  skip_list <- getOption("ds.test.skip_tests")
  skip_if(any(skip_list %in% "auth"))

  source("../../test-cases/set-admin-mode.R")

  auth_token <- set_admin_or_get_token(
    admin_pwd  = getOption("ds.test.admin_pwd"),
    url        = getOption("ds.test.url"),
    skip_tests = skip_list,
    ADMIN_MODE = getOption("ds.test.admin_mode")
  )

  options(ds.test.token = auth_token)

  expect_type(getOption("ds.test.token"), "character")
  expect_gt(nchar(getOption("ds.test.token")), 0)
})