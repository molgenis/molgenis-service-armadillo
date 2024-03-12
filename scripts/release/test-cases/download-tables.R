

download_tables <- function(dest, service_location, skip_tests, default_parquet_path) {
    test_name <- "download-tables"
    if(any(skip_tests %in% test_name)){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

    if(!dir.exists(default_parquet_path)) {
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
    cli_alert_success("Tables ready for testing")
    }
