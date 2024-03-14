create_dir_if_not_exists <- function(directory){
  if (!dir.exists(paste0(dest, directory))) {
    dir.create(paste0(dest, directory))
  }
}

download_test_files <- function(urls, dest){
  n_files <- length(urls)
  cli_progress_bar("Downloading testfiles", total = n_files)
  for (i in 1:n_files) {
    download_url <- urls[i]
    splitted <- strsplit(download_url, "/")[[1]]
    folder <- splitted[length(splitted) - 1]
    filename <- splitted[length(splitted)]
    cli_alert_info(paste0("Downloading ", filename))
    download.file(download_url, paste0(dest, folder, "/", filename), quiet=TRUE)
    cli_progress_update()
  }
  cli_progress_done()
}

download_tables <- function(dest, service_location, skip_tests, default_parquet_path) {
  test_name <- "download-tables"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }

  if (!dir.exists(default_parquet_path)) {
    cli_alert_info("Downloading tables")
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
    cli_alert_success("Tables not downloaded: available locally")
  }
  cli_alert_success(sprintf("%s passed!", test_name))
}
