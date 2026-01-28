# test-21-profiles.R - Profile verification tests
#
# These tests verify that connecting to different profiles works correctly.

# Setup: ensure researcher connection is established
ensure_researcher_login_and_assign()

# Skip all tests if profiles is excluded
skip_if_excluded("profiles")

config <- config

test_that("connection to specific profile works", {
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
