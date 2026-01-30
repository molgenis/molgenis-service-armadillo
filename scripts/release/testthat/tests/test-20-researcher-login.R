# test-20-researcher-login.R - Researcher login tests
#
# These tests verify that researcher can login and establish a connection.

# Setup: ensure admin setup is complete
ensure_admin_setup()

# Helper to check all skip conditions for this test file
skip_if_researcher_login_excluded <- function() {
  skip_if_excluded("researcher-login")
}

test_that("researcher can login and establish connection", {
  skip_if_researcher_login_excluded()
  ensure_researcher_login_and_assign()

  expect_false(is.null(test_env$conns))
  expect_s4_class(test_env$conns[[1]], "ArmadilloConnection")
})
