library(dplyr)

upload_test_data <- function() {
  test_name <- "upload-data"

  test_that("upload test data", {
    do_skip_test(test_name)

    dest <- release_env$dest
    if(dir.exists(release_env$default_parquet_path)){dest <- release_env$default_parquet_path}

    cli_alert_info("Reading parquet files for core variables")
    nonrep <- read_parquet_with_message("core/nonrep", dest)
    yearlyrep <- read_parquet_with_message("core/yearlyrep", dest)
    monthlyrep <- read_parquet_with_message("core/monthlyrep", dest)
    trimesterrep <- read_parquet_with_message("core/trimesterrep", dest)

    cli_alert_info("Uploading core test tables")
    armadillo.upload_table(release_env$project1, "2_1-core-1_0", nonrep)
    armadillo.upload_table(release_env$project1, "2_1-core-1_0", yearlyrep)
    armadillo.upload_table(release_env$project1, "2_1-core-1_0", monthlyrep)
    armadillo.upload_table(release_env$project1, "2_1-core-1_0", trimesterrep)
    cli_alert_success("Uploaded files into core")

    cli_alert_info("Removing temporary core objects")
    rm(nonrep, yearlyrep, monthlyrep, trimesterrep)
    cli_alert_success("Core objects removed")

    cli_alert_info("Reading parquet files for outcome variables")
    nonrep <- read_parquet_with_message("outcome/nonrep", dest)
    yearlyrep <- read_parquet_with_message("outcome/yearlyrep", dest)

    cli_alert_info("Uploading outcome test tables")
    armadillo.upload_table(release_env$project1, "1_1-outcome-1_0", nonrep)
    armadillo.upload_table(release_env$project1, "1_1-outcome-1_0", yearlyrep)
    cli_alert_success("Uploaded files into outcome")

    cli_alert_info("Reading parquet files for survival variables")
    veteran <- read_parquet_with_message("survival/veteran", dest)

    cli_alert_info("Uploading survival test table")
    armadillo.upload_table(release_env$project1, "survival", veteran)
    rm(veteran)
    cli_alert_success("Uploaded files into survival")
    succeed()
  })

  test_that("verify uploaded colnames", {
    do_skip_test(test_name)
    cli_alert_info("Checking if colnames of trimesterrep available")
    trimesterrep <- armadillo.load_table(release_env$project1, "2_1-core-1_0", "trimesterrep")
    cols <- c("row_id", "child_id", "age_trimester", "smk_t", "alc_t")
    expect_identical(colnames(trimesterrep), cols)

    cli_alert_info("Uploading tidyverse test table")
    armadillo.upload_table(release_env$project1, "tidyverse", mtcars)
  })
}
