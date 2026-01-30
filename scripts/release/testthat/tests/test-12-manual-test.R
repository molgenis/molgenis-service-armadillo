# test-12-manual-test.R - Manual UI verification test
#
# This test guides the user through manual browser verification of project
# structure. It is skipped automatically in non-interactive mode (INTERACTIVE=N).

# Setup: ensure tables are uploaded (so project and folders exist to verify)
ensure_tables_uploaded()

config <- test_config

skip_if_manual_excluded <- function() {
  skip_if_excluded("manual-test")
  if (!config$interactive) {
    skip("Non-interactive mode (INTERACTIVE=N)")
  }
}

wait_for_input <- function() {
  cat("\nPress any key to continue")
  readLines("stdin", n = 1)
}

test_that("UI shows project with expected structure", {
  skip_if_manual_excluded()

  cat("\nNow open your testserver in the browser")
  cat(sprintf("\n\nVerify [%s] is available", project))
  wait_for_input()

  cat("\nClick on the icon next to the name to go to the project explorer")
  wait_for_input()

  cat("\nVerify the 1_1-outcome-1_0 and 2_1-core-1_0 folders are there")
  wait_for_input()

  cat("\nVerify core contains nonrep, yearlyrep, monthlyrep and trimesterrep")
  wait_for_input()

  cat("\nVerify outcome contains nonrep and yearlyrep")
  wait_for_input()

  cat("\nWere the manual tests successful? (y/n) ")
  success <- readLines("stdin", n = 1)

  expect_equal(success, "y", label = "Manual UI verification")
})
