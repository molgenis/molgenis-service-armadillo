test_that("Environment is ready", {
  config_url <- getOption("ds.test.url")
  expect_false(is.null(config_url), info = "Global options were not set by configure_test()")
  expect_true(nchar(config_url) > 0)
})