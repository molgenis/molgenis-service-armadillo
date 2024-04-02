prepare_resources <- function(resource_path, url, skip_tests) {
  test_name <- "prepare-resources"
  if (any(skip_tests %in% test_name)) {
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
  }

  if (!file.exists(resource_path)) {
    cli_alert_warning(sprintf("Unable to locate %s, downloading.", resource_path))
    download.file(url, resource_path)
  }

  cli_alert_info("Checking if rda dir exists")
  if (resource_path == "" || !file.exists(resource_path)) {
    exit_test(sprintf("File [%s] doesn't exist", resource_path))
  }
  cli_alert_success(sprintf("%s passed!", test_name))
}
