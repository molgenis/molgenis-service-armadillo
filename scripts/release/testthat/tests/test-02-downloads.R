# test-02-downloads.R - Test data download tests
#
# These tests verify that test tables and resources can be downloaded/found.

# Setup: ensure config is loaded
ensure_config()

test_that("tables can be downloaded or found locally", {
  skip_if_excluded("download-tables")

  ensure_tables_downloaded()

  expect_true(test_env$tables_downloaded)

  # Check that parquet files exist
  config <- test_env$config
  dest <- if (dir.exists(config$default_parquet_path)) {
    config$default_parquet_path
  } else {
    config$dest
  }

  expect_true(
    file.exists(paste0(dest, "core/nonrep.parquet")),
    info = "core/nonrep.parquet should exist"
  )
  expect_true(
    file.exists(paste0(dest, "core/yearlyrep.parquet")),
    info = "core/yearlyrep.parquet should exist"
  )
})

test_that("core parquet files can be read", {
  skip_if_excluded("download-tables")

  ensure_tables_downloaded()

  config <- test_env$config
  dest <- if (dir.exists(config$default_parquet_path)) {
    config$default_parquet_path
  } else {
    config$dest
  }

  nonrep <- arrow::read_parquet(paste0(dest, "core/nonrep.parquet"))
  expect_s3_class(nonrep, "data.frame")
  expect_true(nrow(nonrep) > 0)
})

test_that("survival parquet file exists and can be read", {
  skip_if_excluded("download-tables")

  ensure_tables_downloaded()

  config <- test_env$config
  dest <- if (dir.exists(config$default_parquet_path)) {
    config$default_parquet_path
  } else {
    config$dest
  }

  expect_true(
    file.exists(paste0(dest, "survival/veteran.parquet")),
    info = "survival/veteran.parquet should exist"
  )

  veteran <- arrow::read_parquet(paste0(dest, "survival/veteran.parquet"))
  expect_s3_class(veteran, "data.frame")
  expect_true(nrow(veteran) > 0)
})

test_that("resources can be downloaded or found locally", {
  skip_if_excluded("prepare-resources")

  ensure_resources_downloaded()

  expect_true(test_env$resources_downloaded)

  config <- test_env$config
  expect_true(
    file.exists(config$rda_dir),
    info = sprintf("Resource file should exist: %s", config$rda_dir)
  )
})
