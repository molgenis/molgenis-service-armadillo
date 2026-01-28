# test-01-config.R - Configuration validation tests
#
# These tests validate that the test environment is properly configured.

test_that("configuration loads successfully", {
  ensure_config()

  expect_false(is.null(test_env$config))
  expect_type(test_env$config, "list")
})

test_that("Armadillo URL is valid and accessible", {
  ensure_config()

  url <- test_env$config$armadillo_url
  expect_true(nchar(url) > 0)
  expect_true(startsWith(url, "http"))
  expect_true(RCurl::url.exists(url))
})

test_that("service location is valid", {
  ensure_config()

  service_location <- test_env$config$service_location
  expect_true(dir.exists(service_location))
  expect_true(dir.exists(file.path(service_location, "armadillo")))
})

test_that("authentication is configured", {
  ensure_config()

  # Either admin password or user email must be set
  admin_pwd <- test_env$config$admin_pwd
  user <- test_env$config$user

  expect_true(
    (nchar(admin_pwd) > 0) || (nchar(user) > 0),
    info = "Either admin password or user email must be configured"
  )
})

test_that("profile is configured", {
  ensure_config()

  profile <- test_env$config$profile
  expect_true(nchar(profile) > 0)
})

test_that("app info can be retrieved", {
  ensure_config()

  app_info <- test_env$config$app_info
  expect_false(is.null(app_info))

  version <- test_env$config$version
  # Version may be NULL if actuator/info doesn't return build info
  expect_true(is.null(version) || (length(version) > 0 && nchar(version) > 0))
})

test_that("required libraries are loaded", {
  # Check DataSHIELD/Armadillo libraries

  expect_true("MolgenisArmadillo" %in% loadedNamespaces())
  expect_true("DSI" %in% loadedNamespaces())
  expect_true("dsBaseClient" %in% loadedNamespaces())
  expect_true("DSMolgenisArmadillo" %in% loadedNamespaces())
  expect_true("resourcer" %in% loadedNamespaces())
})
