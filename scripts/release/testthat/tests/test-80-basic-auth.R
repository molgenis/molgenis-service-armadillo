# test-80-basic-auth.R - Basic authentication tests
#
# These tests verify that basic authentication works correctly.
# They run independently and create/delete their own test project.
#
# Note: Unlike other tests, these do ONE login at file level (like the original
# release test) rather than re-logging in each test_that block.

# Setup: just ensure config is loaded (not full admin setup)
ensure_config()

# Helper to check all skip conditions for this test file
skip_if_basic_auth_excluded <- function() {
  skip_if_excluded("basic-auth")
  cfg <- test_env$config
  if (cfg$admin_pwd == "") {
    skip("Admin password not available for basic auth testing")
  }
}

# Do the basic auth login ONCE at file level (before any tests run)
# This mirrors how the original release test worked
#
# Note: We first make a raw httr basic auth call to establish the session,
# because MolgenisArmadillo::armadillo.login_basic() alone doesn't always
# properly override a previous OIDC session. This replicates what happens
# when exposome/omics tests run (they call set_dm_permissions which uses
# raw httr basic auth).
cfg <- test_env$config
if (cfg$admin_pwd != "") {
  # First, establish basic auth via raw httr (like set_dm_permissions does)
  auth_header <- get_auth_header("basic", cfg$admin_pwd)
  httr::GET(
    paste0(cfg$armadillo_url, "actuator/info"),
    config = httr::add_headers(auth_header)
  )
  # Then do the MolgenisArmadillo login
  MolgenisArmadillo::armadillo.login_basic(
    cfg$armadillo_url,
    "admin",
    cfg$admin_pwd
  )
}

test_that("admin can login with basic authentication", {
  skip_if_basic_auth_excluded()
  # Login already happened above, just verify we can list projects
  expect_no_error({
    MolgenisArmadillo::armadillo.list_projects()
  })
})

test_that("admin can create project with basic auth", {
  skip_if_basic_auth_excluded()
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
  skip_if_basic_auth_excluded()
  project <- test_env$basic_auth_project
  cfg <- test_env$config

  # Read parquet file
  dest <- if (dir.exists(cfg$default_parquet_path)) {
    cfg$default_parquet_path
  } else {
    cfg$dest
  }

  nonrep <- arrow::read_parquet(paste0(dest, "core/nonrep.parquet"))

  expect_no_error({
    MolgenisArmadillo::armadillo.upload_table(project, "2_1-core-1_0", nonrep)
  })

  rm(nonrep)
})

test_that("uploaded table exists", {
  skip_if_basic_auth_excluded()
  project <- test_env$basic_auth_project
  table <- sprintf("%s/2_1-core-1_0/nonrep", project)

  tables <- MolgenisArmadillo::armadillo.list_tables(project)

  expect_true(
    table %in% tables,
    info = sprintf("Table %s should exist", table)
  )
})

test_that("admin can delete project with basic auth", {
  skip_if_basic_auth_excluded()
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
