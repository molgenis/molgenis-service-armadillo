library(dplyr)

# Setup
test_name <- "upload-data"

test_that("upload test data", {
  do_skip_test(test_name)

  dest <- get_tables_path()

  nonrep <- read_parquet_with_message("core/nonrep", dest)
  yearlyrep <- read_parquet_with_message("core/yearlyrep", dest)
  monthlyrep <- read_parquet_with_message("core/monthlyrep", dest)
  trimesterrep <- read_parquet_with_message("core/trimesterrep", dest)

  armadillo.upload_table(release_env$project1, "2_1-core-1_0", nonrep)
  armadillo.upload_table(release_env$project1, "2_1-core-1_0", yearlyrep)
  armadillo.upload_table(release_env$project1, "2_1-core-1_0", monthlyrep)
  armadillo.upload_table(release_env$project1, "2_1-core-1_0", trimesterrep)

  rm(nonrep, yearlyrep, monthlyrep, trimesterrep)

  nonrep <- read_parquet_with_message("outcome/nonrep", dest)
  yearlyrep <- read_parquet_with_message("outcome/yearlyrep", dest)

  armadillo.upload_table(release_env$project1, "1_1-outcome-1_0", nonrep)
  armadillo.upload_table(release_env$project1, "1_1-outcome-1_0", yearlyrep)

  veteran <- read_parquet_with_message("survival/veteran", dest)

  armadillo.upload_table(release_env$project1, "survival", veteran)
  rm(veteran)
  succeed()
})

test_that("verify uploaded colnames", {
  do_skip_test(test_name)
  trimesterrep <- armadillo.load_table(release_env$project1, "2_1-core-1_0", "trimesterrep")
  cols <- c("row_id", "child_id", "age_trimester", "smk_t", "alc_t")
  expect_identical(colnames(trimesterrep), cols)
})

test_that("upload tidyverse test data", {
  do_skip_test(test_name)
  armadillo.upload_table(release_env$project1, "tidyverse", mtcars)
  # Create grouped version for group_by tests

  mtcars_group <- mtcars %>% group_by(cyl)
  armadillo.upload_table(release_env$project1, "tidyverse", mtcars_group)
  succeed()
})
