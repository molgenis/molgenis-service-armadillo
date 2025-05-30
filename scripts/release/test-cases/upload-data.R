library(dplyr)

upload_test_data <- function(project, dest, default_parquet_path, skip_tests) {
  test_name <- "upload-data"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }
  
  if(dir.exists(default_parquet_path)){dest <- default_parquet_path}
  
  cli_alert_info("Reading parquet files for core variables")
  nonrep <- read_parquet_with_message("core/nonrep", dest)
  yearlyrep <- read_parquet_with_message("core/yearlyrep", dest)
  monthlyrep <- read_parquet_with_message("core/monthlyrep", dest)
  trimesterrep <- read_parquet_with_message("core/trimesterrep", dest)

  cli_alert_info("Uploading core test tables")
  armadillo.upload_table(project, "2_1-core-1_0", nonrep)
  armadillo.upload_table(project, "2_1-core-1_0", yearlyrep)
  armadillo.upload_table(project, "2_1-core-1_0", monthlyrep)
  armadillo.upload_table(project, "2_1-core-1_0", trimesterrep)
  cli_alert_success("Uploaded files into core")

  cli_alert_info("Removing temporary core objects")
  rm(nonrep, yearlyrep, monthlyrep, trimesterrep)
  cli_alert_success("Core objects removed")

  cli_alert_info("Reading parquet files for outcome variables")
  nonrep <- read_parquet_with_message("outcome/nonrep", dest)
  yearlyrep <- read_parquet_with_message("outcome/yearlyrep", dest)

  cli_alert_info("Uploading outcome test tables")
  armadillo.upload_table(project, "1_1-outcome-1_0", nonrep)
  armadillo.upload_table(project, "1_1-outcome-1_0", yearlyrep)
  cli_alert_success("Uploaded files into outcome")

  if (!any(skip_tests %in% test_name)) {
    cli_alert_info("Reading parquet files for survival variables")
    veteran <- read_parquet_with_message("survival/veteran", dest)
  }

  cli_alert_info("Uploading survival test table")
  armadillo.upload_table(project, "survival", veteran)
  rm(veteran)
  cli_alert_success("Uploaded files into survival")

  cli_alert_info("Checking if colnames of trimesterrep available")
  trimesterrep <- armadillo.load_table(project, "2_1-core-1_0", "trimesterrep")
  cols <- c("row_id", "child_id", "age_trimester", "smk_t", "alc_t")
  if (identical(colnames(trimesterrep), cols)) {
    cli_alert_success("Colnames correct")
  } else {
    cli_alert_danger(paste0(colnames(trimesterrep), "!=", cols))
    exit_test("Colnames incorrect")
  }
  
  cli_alert_info("Uploading tidyverse test table")
  armadillo.upload_table(project, "tidyverse", mtcars)
  cli_alert_success(sprintf("%s passed!", test_name))
}
