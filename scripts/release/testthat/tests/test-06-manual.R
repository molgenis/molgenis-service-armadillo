# Setup
test_name <- "manual-test"

test_that("manual UI verification", {
  do_skip_test(test_name)

  cat("\nNow open your testserver in the browser")
  cat(sprintf("\n\nVerify [%s] is available", release_env$project1))
  wait_for_input(release_env$interactive)
  cat("\nClick on the icon next to the name to go to the project explorer")
  wait_for_input(release_env$interactive)
  cat("\nVerify the 1_1-outcome-1_0 and 2_1-core-1_0 folders are there")
  wait_for_input(release_env$interactive)
  cat("\nVerify core contains nonrep, yearlyrep, monthlyrep and trimesterrep")
  wait_for_input(release_env$interactive)
  cat("\nVerify outcome contains nonrep and yearlyrep")
  wait_for_input(release_env$interactive)

  if (release_env$interactive) {
    cat("\nWere the manual tests successful? (y/n) ")
    success <- readLines("stdin", n = 1)
    expect_equal(success, "y", info = "Manual tests failed: problem in UI")
  } else {
    succeed()
  }
})
