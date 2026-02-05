library(purrr)

# Setup
test_name <- "remove-data"

test_that("delete tables", {
  do_skip_test(test_name)
  set_dm_permissions()
  expect_no_error({
    armadillo.delete_table(release_env$project1, "2_1-core-1_0", "nonrep")
    armadillo.delete_table(release_env$project1, "2_1-core-1_0", "yearlyrep")
    armadillo.delete_table(release_env$project1, "2_1-core-1_0", "trimesterrep")
    armadillo.delete_table(release_env$project1, "2_1-core-1_0", "monthlyrep")
    armadillo.delete_table(release_env$project1, "1_1-outcome-1_0", "nonrep")
    armadillo.delete_table(release_env$project1, "1_1-outcome-1_0", "yearlyrep")
  })
})

test_that("delete projects", {
  do_skip_test(test_name)
  cat(sprintf("\nVerify in UI all data from [%s] is gone.", release_env$project1))
  wait_for_input(release_env$interactive)
  expect_no_error(armadillo.delete_project(release_env$project1))
  wait_for_input(release_env$interactive)
})
