read_parquet_with_message <- function(file_path) {
    cli_alert_info(file_path)
    out <- arrow::read_parquet(paste0(dest, paste0(file_path, ".parquet")))
    cli_alert_success(paste0(file_path, " read"))
    return(out)
    }

cli_alert_info("Reading parquet files for core variables")
nonrep <- read_parquet_with_message("core/nonrep")
yearlyrep <- read_parquet_with_message("core/yearlyrep")
monthlyrep <- read_parquet_with_message("core/monthlyrep")
trimesterrep <- read_parquet_with_message("core/trimesterrep")

cli_alert_info("Uploading core test tables")
armadillo.upload_table(project1, "2_1-core-1_0", nonrep)
armadillo.upload_table(project1, "2_1-core-1_0", yearlyrep)
armadillo.upload_table(project1, "2_1-core-1_0", monthlyrep)
armadillo.upload_table(project1, "2_1-core-1_0", trimesterrep)
cli_alert_success("Uploaded files into core")

cli_alert_info("Removing temporary core objects")
rm(nonrep, yearlyrep, monthlyrep, trimesterrep)
cli_alert_success("Core objects removed")

cli_alert_info("Reading parquet files for outcome variables")
nonrep <- read_parquet_with_message("outcome/nonrep")
yearlyrep <- read_parquet_with_message("outcome/yearlyrep")

cli_alert_info("Uploading outcome test tables")
armadillo.upload_table(project1, "1_1-outcome-1_0", nonrep)
armadillo.upload_table(project1, "1_1-outcome-1_0", yearlyrep)
cli_alert_success("Uploaded files into outcome")

cli_alert_info("Reading parquet files for survival variables")
veteran <- read_parquet_with_message("survival/veteran")

# cli_alert_info("Logging in as admin user")
# armadillo.login_basic(armadillo_url, "admin", admin_pwd)

cli_alert_info("Uploading survival test table")
armadillo.upload_table(project1, "survival", veteran)
rm(veteran)
cli_alert_success("Uploaded files into survival")

cli_alert_info("Checking if colnames of trimesterrep available")
trimesterrep <- armadillo.load_table(project1, "2_1-core-1_0", "trimesterrep")
cols <- c("row_id","child_id","age_trimester","smk_t","alc_t")
if (identical(colnames(trimesterrep), cols)){
  cli_alert_success("Colnames correct")
} else {
  cli_alert_danger(paste0(colnames(trimesterrep), "!=", cols))
  exit_test("Colnames incorrect")
}