default_parquet_path = file.path(service_location, "data", "shared-lifecycle")

if(!dir.exists(default_parquet_path)) {
  cli_alert_info("Downloading tables")
  dest <- add_slash_if_not_added(test_file_path)
  cli_alert_danger(paste0("Unable to locate data/lifecycle, attempting to download test files into: ", dest))
  create_dir_if_not_exists("core")
  create_dir_if_not_exists("outcome")
  test_files_url_template <- "https://github.com/molgenis/molgenis-service-armadillo/raw/master/data/shared-lifecycle/%s/%srep.parquet"
  download_test_files(
    c(
      sprintf(test_files_url_template, "core", "non"),
      sprintf(test_files_url_template, "core", "yearly"),
      sprintf(test_files_url_template, "core", "monthly"),
      sprintf(test_files_url_template, "core", "trimester"),
      sprintf(test_files_url_template, "outcome", "non"),
      sprintf(test_files_url_template, "outcome", "yearly")
    ),
    dest
  )
  cli_alert_success("Tables downloaded")
} else {
  dest <- add_slash_if_not_added(default_parquet_path)
  cli_alert_success("Tables not downloaded: available locally")
}
