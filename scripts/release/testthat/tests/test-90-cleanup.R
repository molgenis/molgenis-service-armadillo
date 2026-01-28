# test-90-cleanup.R - Cleanup tests
#
# These tests handle cleanup of test data created during the test run.
# They should run last to clean up the test project.

# Setup: ensure admin setup was done (so we have a project to clean up)
ensure_admin_setup()

# Skip all tests if remove-data is excluded
skip_if_excluded("remove-data")

test_that("DM permissions can be reset", {
  config <- test_env$config

  if (!config$ADMIN_MODE && config$update_auto == "y") {
    expect_no_error({
      set_user(
        user = config$user,
        admin_pwd = config$admin_pwd,
        isAdmin = TRUE,
        required_projects = list(test_env$project),
        url = config$armadillo_url
      )
    })
  }
})

test_that("core tables can be deleted", {
  project <- test_env$project

  # Delete core tables
  expect_no_error({
    MolgenisArmadillo::armadillo.delete_table(project, "2_1-core-1_0", "nonrep")
  })

  expect_no_error({
    MolgenisArmadillo::armadillo.delete_table(project, "2_1-core-1_0", "yearlyrep")
  })

  expect_no_error({
    MolgenisArmadillo::armadillo.delete_table(project, "2_1-core-1_0", "trimesterrep")
  })

  expect_no_error({
    MolgenisArmadillo::armadillo.delete_table(project, "2_1-core-1_0", "monthlyrep")
  })
})

test_that("outcome tables can be deleted", {
  project <- test_env$project

  expect_no_error({
    MolgenisArmadillo::armadillo.delete_table(project, "1_1-outcome-1_0", "nonrep")
  })

  expect_no_error({
    MolgenisArmadillo::armadillo.delete_table(project, "1_1-outcome-1_0", "yearlyrep")
  })
})

test_that("test project can be deleted", {
  project <- test_env$project

  expect_no_error({
    MolgenisArmadillo::armadillo.delete_project(project)
  })

  # Verify project is gone
  projects <- MolgenisArmadillo::armadillo.list_projects()

  expect_false(
    project %in% projects,
    info = sprintf("Project %s should be deleted", project)
  )
})
