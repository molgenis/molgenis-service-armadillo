# test-20-researcher.R - Researcher login and data access tests
#
# These tests verify that:
# - Researcher can login
# - Profiles can be verified
# - Tables can be assigned
# - Resources can be accessed

# Setup: ensure admin setup is complete
ensure_admin_setup()

test_that("researcher can login and establish connection", {
  skip_if_excluded("researcher_login")

  ensure_researcher_login()

  expect_false(is.null(test_env$conns))
  expect_s4_class(test_env$conns[[1]], "ArmadilloConnection")
})

test_that("connection to specific profile works", {
  skip_if_excluded("verify-profile")

  ensure_researcher_login()

  config <- config()

  # Create a test connection to verify profile connectivity
  if (config$ADMIN_MODE) {
    con <- DSMolgenisArmadillo::dsConnect(
      drv = DSMolgenisArmadillo::armadillo(),
      name = "armadillo",
      user = "admin",
      password = config$admin_pwd,
      url = config$armadillo_url,
      profile = config$profile
    )
  } else {
    con <- DSMolgenisArmadillo::dsConnect(
      drv = DSMolgenisArmadillo::armadillo(),
      name = "armadillo",
      token = test_env$token,
      url = config$armadillo_url,
      profile = config$profile
    )
  }

  expect_equal(con@name, "armadillo")
  DSI::dsDisconnect(con)
})

test_that("connection without profile specification works", {
  skip_if_excluded("verify-profile")

  ensure_researcher_login()

  config <- config()

  if (config$ADMIN_MODE) {
    con <- DSMolgenisArmadillo::dsConnect(
      drv = DSMolgenisArmadillo::armadillo(),
      name = "armadillo",
      user = "admin",
      password = config$admin_pwd,
      url = config$armadillo_url,
      profile = ""
    )
  } else {
    con <- DSMolgenisArmadillo::dsConnect(
      drv = DSMolgenisArmadillo::armadillo(),
      name = "armadillo",
      token = test_env$token,
      url = config$armadillo_url,
      profile = ""
    )
  }

  expect_equal(con@name, "armadillo")
  DSI::dsDisconnect(con)
})

test_that("connection to default profile works", {
  skip_if_excluded("verify-profile")

  ensure_researcher_login()

  config <- config()

  if (config$ADMIN_MODE) {
    con <- DSMolgenisArmadillo::dsConnect(
      drv = DSMolgenisArmadillo::armadillo(),
      name = "armadillo",
      user = "admin",
      password = config$admin_pwd,
      url = config$armadillo_url,
      profile = "default"
    )
  } else {
    con <- DSMolgenisArmadillo::dsConnect(
      drv = DSMolgenisArmadillo::armadillo(),
      name = "armadillo",
      token = test_env$token,
      url = config$armadillo_url,
      profile = "default"
    )
  }

  expect_equal(con@name, "armadillo")
  DSI::dsDisconnect(con)
})

test_that("table can be assigned", {
  skip_if_excluded("assigning")

  ensure_researcher_login()


  # Assign a table
  DSI::datashield.assign.table(
    conns(),
    "test_nonrep",
    sprintf("%s/2_1-core-1_0/nonrep", project())
  )

  # Verify it's a data frame
  datatype <- dsBaseClient::ds.class(x = "test_nonrep", datasources = conns())

  expect_equal(datatype$armadillo, "data.frame")
})

test_that("expression can be assigned", {
  skip_if_excluded("assigning")

  ensure_researcher_login()

  # Assign an expression (extracting a column)
  expect_no_error({
    DSI::datashield.assign.expr(
      conns(),
      "test_x",
      expr = as.symbol("nonrep$coh_country")
    )
  })
})

test_that("resource can be seen", {
  skip_if_excluded("verify-resources")

  ensure_researcher_login()

  config <- config()

  # Skip if in admin mode (resources don't work with basic auth)
  if (config$ADMIN_MODE) {
    skip("Cannot test resources with basic authentication")
  }

  # Skip if resourcer not available for profile
  if (!"resourcer" %in% test_env$profile_info$packageWhitelist) {
    skip(sprintf("Resourcer not available for profile: %s", config$profile))
  }

  full_resource_path <- sprintf("%s/ewas/GSE66351_1", project())

  resources <- DSI::datashield.resources(conns = conns())

  expect_true(
    full_resource_path %in% resources$armadillo,
    info = sprintf("Resource %s should be visible", full_resource_path)
  )
})

test_that("resource can be assigned", {
  skip_if_excluded("verify-resources")

  ensure_researcher_login()

  config <- config()

  if (config$ADMIN_MODE) {
    skip("Cannot test resources with basic authentication")
  }

  if (!"resourcer" %in% test_env$profile_info$packageWhitelist) {
    skip(sprintf("Resourcer not available for profile: %s", config$profile))
  }

  full_resource_path <- sprintf("%s/ewas/GSE66351_1", project())

  # Assign the resource
  DSI::datashield.assign.resource(
    conns(),
    resource = full_resource_path,
    symbol = "eSet_0y_EUR"
  )

  # Check class
  resource_class <- dsBaseClient::ds.class("eSet_0y_EUR", datasources = conns())

  expected <- c("RDataFileResourceClient", "FileResourceClient", "ResourceClient", "R6")

  # Check that all expected classes are present
  expect_true(
    length(setdiff(expected, resource_class$armadillo)) == 0,
    info = "Resource should have expected class"
  )
})
