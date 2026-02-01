create_resource <- function(folder, file_name, resource_name, format) {
  test_name <- "create_resource"
  if (do_skip_test(test_name)) {
    return()
  }

  rds_url <- release_env$armadillo_url
  if (release_env$armadillo_url == "http://localhost:8080/") {
    rds_url <- "http://host.docker.internal:8080/"
  }

  created_resource <- resourcer::newResource(
    name = resource_name,
    url = sprintf("%sstorage/projects/%s/objects/%s%s%s", rds_url, release_env$project1, folder, "%2F", file_name),
    format = format
  )
  cli_alert_success(sprintf("%s passed!", test_name))
  return(created_resource)
}
