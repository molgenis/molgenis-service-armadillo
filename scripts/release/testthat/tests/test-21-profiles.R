# test-21-profiles.R - Profile verification tests
#
# These tests verify that connecting to different profiles works correctly.

# Setup: ensure researcher connection is established
ensure_researcher_login_and_assign()

config <- test_config

# Helper to check all skip conditions for this test file
skip_if_profiles_excluded <- function() {
  skip_if_excluded("profiles")
}

test_that("connection to specific profile works", {
  skip_if_profiles_excluded()
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
  skip_if_profiles_excluded()
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
  skip_if_profiles_excluded()
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
