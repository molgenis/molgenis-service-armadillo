# test-80-basic-auth.R - Basic authentication tests
#
# These tests verify that basic authentication works correctly.
# They run independently and create/delete their own test project.

# Setup: just ensure config is loaded (not full admin setup)
ensure_config

# Skip all tests if basic-auth is excluded
skip_if_excluded("basic-auth")

config <- test_config

# Skip if admin password is not available
if (config$admin_pwd == "") {
  skip("Admin password not available for basic auth testing")
}

test_that("admin can login with basic authentication", {
  expect_no_error({
    MolgenisArmadillo::armadillo.login_basic(
      config$armadillo_url,
      "admin",
      config$admin_pwd
    )
  })
})

test_that("admin can create project with basic auth", {
  # Generate unique project name for this test
  current_projects <- MolgenisArmadillo::armadillo.list_projects()
  basic_auth_project <- stringi::stri_rand_strings(1, 10, "[a-z0-9]")
  while (basic_auth_project %in% current_projects) {
    basic_auth_project <- stringi::stri_rand_strings(1, 10, "[a-z0-9]")
  }

  # Store for later tests
  test_env$basic_auth_project <- basic_auth_project

  expect_no_error({
    MolgenisArmadillo::armadillo.create_project(basic_auth_project)
  })

  # Verify project exists
  projects <- MolgenisArmadillo::armadillo.list_projects()
  expect_true(
    basic_auth_project %in% projects,
    info = sprintf("Project %s should exist", basic_auth_project)
  )
})

test_that("admin can upload data with basic auth", {
  project <- test_env$basic_auth_project

  # Read parquet file
  dest <- if (dir.exists(config$default_parquet_path)) {
    config$default_parquet_path
  } else {
    config$dest
  }

  nonrep <- arrow::read_parquet(paste0(dest, "core/nonrep.parquet"))

  expect_no_error({
    MolgenisArmadillo::armadillo.upload_table(project, "2_1-core-1_0", nonrep)
  })

  rm(nonrep)
})

test_that("uploaded table exists", {
  project <- test_env$basic_auth_project
  table <- sprintf("%s/2_1-core-1_0/nonrep", project)

  tables <- MolgenisArmadillo::armadillo.list_tables(project)

  expect_true(
    table %in% tables,
    info = sprintf("Table %s should exist", table)
  )
})

test_that("admin can delete project with basic auth", {
  project <- test_env$basic_auth_project

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
