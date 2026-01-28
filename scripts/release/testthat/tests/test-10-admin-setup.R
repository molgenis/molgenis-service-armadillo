# test-10-admin-setup.R - Admin setup and data upload tests
#
# These tests verify that:
# - Data manager can login
# - Projects can be created
# - Test data can be uploaded

# Setup: ensure downloads are complete
ensure_tables_downloaded()
ensure_resources_downloaded()

test_that("data manager can login", {
  skip_if_excluded("dm_login")

  # Use ensure_admin_setup which handles login - don't call login separately
  # as that would request another token
  ensure_admin_setup()

  # Verify we have a valid token/session
  expect_true(test_env$admin_setup)
  expect_false(is.null(test_env$token))
})

test_that("random project name can be generated", {
  skip_if_excluded("generate-project")

  current_projects <- MolgenisArmadillo::armadillo.list_projects()
  random_project <- stringi::stri_rand_strings(1, 10, "[a-z0-9]")

  expect_true(nchar(random_project) == 10)
  expect_true(grepl("^[a-z0-9]+$", random_project))
})

test_that("admin setup completes successfully", {
  skip_if_excluded("create-test-project")
  skip_if_excluded("upload-data")

  ensure_admin_setup()

  expect_true(test_env$admin_setup)
  expect_false(is.null(test_env$project))
  expect_true(nchar(test_env$project) > 0)
})

test_that("test project exists after creation", {
  skip_if_excluded("create-test-project")

  ensure_admin_setup()

  projects <- MolgenisArmadillo::armadillo.list_projects()
  expect_true(
    test_env$project %in% projects,
    info = sprintf("Project %s should exist", test_env$project)
  )
})

test_that("core tables are uploaded", {
  skip_if_excluded("upload-data")

  ensure_admin_setup()

  tables <- MolgenisArmadillo::armadillo.list_tables(test_env$project)
  expected_tables <- c(
    "2_1-core-1_0/nonrep",
    "2_1-core-1_0/yearlyrep",
    "2_1-core-1_0/monthlyrep",
    "2_1-core-1_0/trimesterrep"
  )

  for (table in expected_tables) {
    full_table <- sprintf("%s/%s", test_env$project, table)
    expect_true(
      full_table %in% tables,
      info = sprintf("Table %s should exist", table)
    )
  }
})

test_that("outcome tables are uploaded", {
  skip_if_excluded("upload-data")

  ensure_admin_setup()

  tables <- MolgenisArmadillo::armadillo.list_tables(test_env$project)
  expected_tables <- c(
    "1_1-outcome-1_0/nonrep",
    "1_1-outcome-1_0/yearlyrep"
  )

  for (table in expected_tables) {
    full_table <- sprintf("%s/%s", test_env$project, table)
    expect_true(
      full_table %in% tables,
      info = sprintf("Table %s should exist", table)
    )
  }
})

test_that("survival table is uploaded", {
  skip_if_excluded("upload-data")

  ensure_admin_setup()

  tables <- MolgenisArmadillo::armadillo.list_tables(test_env$project)
  full_table <- sprintf("%s/survival/veteran", test_env$project)

  expect_true(
    full_table %in% tables,
    info = "survival/veteran table should exist"
  )
})

test_that("tidyverse table is uploaded", {
  skip_if_excluded("upload-data")

  ensure_admin_setup()

  tables <- MolgenisArmadillo::armadillo.list_tables(test_env$project)
  full_table <- sprintf("%s/tidyverse/mtcars", test_env$project)

  expect_true(
    full_table %in% tables,
    info = "tidyverse/mtcars table should exist"
  )
})

test_that("trimesterrep has correct column names", {
  skip_if_excluded("upload-data")

  ensure_admin_setup()

  trimesterrep <- MolgenisArmadillo::armadillo.load_table(
    test_env$project,
    "2_1-core-1_0",
    "trimesterrep"
  )

  expected_cols <- c("row_id", "child_id", "age_trimester", "smk_t", "alc_t")
  expect_identical(colnames(trimesterrep), expected_cols)
})

test_that("resource is created and uploaded", {
  skip_if_excluded("upload-resource")
  skip_if_excluded("create-resource")

  ensure_admin_setup()

  # Resource should be available after admin setup
  # We'll verify this more thoroughly in the researcher tests
  expect_true(test_env$admin_setup)
})
