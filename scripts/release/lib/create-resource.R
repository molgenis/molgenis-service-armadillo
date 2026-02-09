create_resource <- function(folder, file_name, resource_name, format) {
  rds_url <- release_env$armadillo_url
  if (release_env$armadillo_url == "http://localhost:8080/") {
    rds_url <- "http://host.docker.internal:8080/"
  }

  resourcer::newResource(
    name = resource_name,
    url = sprintf("%sstorage/projects/%s/objects/%s%s%s", rds_url, release_env$project1, folder, "%2F", file_name),
    format = format
  )
}
