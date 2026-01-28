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

# Before basic auth tests, replicate the cleanup sequence from the original
# release test. The original calls dm_clean_up() then datashield.logout() before
# verify_basic_auth(). The critical step is restoring admin permissions to the
# OIDC user, which set_researcher_access() removed during researcher test setup.
# Without this, armadillo.login_basic() does not fully override the existing OIDC
# session, and subsequent API calls use the non-admin OIDC credentials (403).

# Step 1: Restore admin permissions for the OIDC user (mirrors dm_clean_up)
cfg <- test_env$config
if (isTRUE(test_env$researcher_permissions_set) && !cfg$ADMIN_MODE && cfg$admin_pwd != "") {
  set_user(
    user = cfg$user,
    admin_pwd = cfg$admin_pwd,
    isAdmin = TRUE,
    required_projects = if (!is.null(test_env$project)) list(test_env$project) else list(),
    url = cfg$armadillo_url
  )
}

# Step 2: Clear DSI/DataSHIELD connection state
if (!is.null(test_env$conns)) {
  tryCatch(
    suppressMessages(DSI::datashield.logout(test_env$conns)),
    error = function(e) NULL
  )
  test_env$conns <- NULL
}

# Step 3: Basic auth login (once at file level, like the original release test)
if (cfg$admin_pwd != "") {
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
