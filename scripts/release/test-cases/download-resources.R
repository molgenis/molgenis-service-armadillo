prepare_resources <- function(resource_path = release_env$rda_dir, url = release_env$rda_url) {
  test_name <- "prepare-resources"
  if (should_skip_test(test_name)) {
    return()
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
