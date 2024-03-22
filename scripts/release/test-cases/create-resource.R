create_resource <- function(target_project, url, folder, file_name, resource_name, format, skip_tests) {
  test_name <- "create_resource"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }

  rds_url <- url
  if (url == "http://localhost:8080/") {
    rds_url <- "http://host.docker.internal:8080/"
  }

  created_resource <- resourcer::newResource(
    name = resource_name,
    url = sprintf("%sstorage/projects/%s/objects/%s%s%s", rds_url, target_project, folder, "%2F", file_name),
    format = format
  )
  cli_alert_success(sprintf("%s passed!", test_name))
  return(created_resource)
}
