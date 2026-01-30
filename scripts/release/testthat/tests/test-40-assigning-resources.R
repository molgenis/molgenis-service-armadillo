# test-23-assigning-resources.R - Resource assignment tests
#
# These tests verify that resources can be accessed and assigned.

# Setup: ensure researcher connection is established (resources mode - no table download)
ensure_researcher_login()
ensure_resources_uploaded()

# Get config for skip checks
config <- test_config

# Helper to check all skip conditions for this test file
skip_if_resources_excluded <- function() {
  skip_if_excluded("assigning-resources")
  if (config$ADMIN_MODE) {
    skip("Cannot test resources with basic authentication")
  }
  if (!"resourcer" %in% test_env$profile_info$packageWhitelist) {
    skip(sprintf("Resourcer not available for profile: %s", config$profile))
  }
}

test_that("resource can be seen", {
  skip_if_resources_excluded()
  full_resource_path <- sprintf("%s/ewas/GSE66351_1", project)

  resources <- suppressMessages(DSI::datashield.resources(conns = conns))

  expect_true(
    full_resource_path %in% resources$armadillo,
    info = sprintf("Resource %s should be visible", full_resource_path)
  )
})

# Note: suppressMessages hides "Data in all studies were valid" which is expected behavior

test_that("resource can be assigned", {
  skip_if_resources_excluded()
  full_resource_path <- sprintf("%s/ewas/GSE66351_1", project)

  # Assign the resource
  suppressMessages(DSI::datashield.assign.resource(
    conns,
    resource = full_resource_path,
    symbol = "eSet_0y_EUR"
  ))

  # Check class
  resource_class <- suppressMessages(dsBaseClient::ds.class("eSet_0y_EUR", datasources = conns))

  expected <- c("RDataFileResourceClient", "FileResourceClient", "ResourceClient", "R6")

  # Check that all expected classes are present
  expect_true(
    length(setdiff(expected, resource_class$armadillo)) == 0,
    info = "Resource should have expected class"
  )
})
