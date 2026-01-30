# test-10-upload-tables.R - Data upload and project setup tests
#
# These tests verify that:
# - Admin can login
# - Projects can be created
# - Test data can be uploaded

# Setup: ensure prerequisites (config, downloads, project creation, admin login)
ensure_tables_downloaded()
ensure_project_created()

test_that("random project name can be generated", {
  skip_if_excluded("upload-tables")

  current_projects <- MolgenisArmadillo::armadillo.list_projects()
  random_project <- stringi::stri_rand_strings(1, 10, "[a-z0-9]")

  expect_true(nchar(random_project) == 10)
  expect_true(grepl("^[a-z0-9]+$", random_project))
})

test_that("test project exists after creation", {
  skip_if_excluded("upload-tables")

  projects <- MolgenisArmadillo::armadillo.list_projects()
  expect_true(
    test_env$project %in% projects,
    info = sprintf("Project %s should exist", test_env$project)
  )
})

test_that("core tables can be uploaded", {
  skip_if_excluded("upload-tables")

  config <- test_env$config
  dest <- if (dir.exists(config$default_parquet_path)) config$default_parquet_path else config$dest

  # Load and upload core tables
  nonrep <- arrow::read_parquet(paste0(dest, "core/nonrep.parquet"))
  yearlyrep <- arrow::read_parquet(paste0(dest, "core/yearlyrep.parquet"))
  monthlyrep <- arrow::read_parquet(paste0(dest, "core/monthlyrep.parquet"))
  trimesterrep <- arrow::read_parquet(paste0(dest, "core/trimesterrep.parquet"))

  # Validate data integrity before upload
  expected_cols <- c("row_id", "child_id", "age_trimester", "smk_t", "alc_t")
  expect_identical(colnames(trimesterrep), expected_cols,
    info = "Trimesterrep should have expected columns")

  MolgenisArmadillo::armadillo.upload_table(test_env$project, "2_1-core-1_0", nonrep)
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "2_1-core-1_0", yearlyrep)
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "2_1-core-1_0", monthlyrep)
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "2_1-core-1_0", trimesterrep)
  rm(nonrep, yearlyrep, monthlyrep, trimesterrep)

  # Verify tables exist
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
      info = sprintf("Table %s should exist after upload", table)
    )
  }

  # Mark as uploaded for other tests/ensure functions
  test_env$tables_uploaded <- TRUE
})

test_that("outcome tables can be uploaded", {
  skip_if_excluded("upload-tables")

  config <- test_env$config
  dest <- if (dir.exists(config$default_parquet_path)) config$default_parquet_path else config$dest

  # Load and upload outcome tables
  nonrep <- arrow::read_parquet(paste0(dest, "outcome/nonrep.parquet"))
  yearlyrep <- arrow::read_parquet(paste0(dest, "outcome/yearlyrep.parquet"))

  MolgenisArmadillo::armadillo.upload_table(test_env$project, "1_1-outcome-1_0", nonrep)
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "1_1-outcome-1_0", yearlyrep)
  rm(nonrep, yearlyrep)

  # Verify tables exist
  tables <- MolgenisArmadillo::armadillo.list_tables(test_env$project)
  expected_tables <- c(
    "1_1-outcome-1_0/nonrep",
    "1_1-outcome-1_0/yearlyrep"
  )

  for (table in expected_tables) {
    full_table <- sprintf("%s/%s", test_env$project, table)
    expect_true(
      full_table %in% tables,
      info = sprintf("Table %s should exist after upload", table)
    )
  }
})

test_that("survival table can be uploaded", {
  skip_if_excluded("upload-tables")

  config <- test_env$config
  dest <- if (dir.exists(config$default_parquet_path)) config$default_parquet_path else config$dest

  # Load and upload survival table
  veteran <- arrow::read_parquet(paste0(dest, "survival/veteran.parquet"))
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "survival", veteran)
  rm(veteran)

  # Verify table exists
  tables <- MolgenisArmadillo::armadillo.list_tables(test_env$project)
  full_table <- sprintf("%s/survival/veteran", test_env$project)

  expect_true(
    full_table %in% tables,
    info = "survival/veteran table should exist after upload"
  )
})

test_that("tidyverse table can be uploaded", {
  skip_if_excluded("upload-tables")

  # Upload tidyverse table (mtcars is built-in to R)
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "tidyverse", mtcars)

  # Verify table exists
  tables <- MolgenisArmadillo::armadillo.list_tables(test_env$project)
  full_table <- sprintf("%s/tidyverse/mtcars", test_env$project)

  expect_true(
    full_table %in% tables,
    info = "tidyverse/mtcars table should exist after upload"
  )
})

test_that("uploaded table has correct structure", {
  skip_if_excluded("upload-tables")

  # Verify we can load and inspect an uploaded table
  trimesterrep <- MolgenisArmadillo::armadillo.load_table(
    test_env$project,
    "2_1-core-1_0",
    "trimesterrep"
  )

  expected_cols <- c("row_id", "child_id", "age_trimester", "smk_t", "alc_t")
  expect_identical(colnames(trimesterrep), expected_cols,
    info = "Loaded table should preserve column structure")
})

