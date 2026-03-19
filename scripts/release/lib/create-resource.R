create_resource <- function(folder, file_name, resource_name, format,
                            target_project = release_env$project1,
                            use_rawfiles = FALSE) {
  rds_url <- release_env$armadillo_url
  if (release_env$armadillo_url == "http://localhost:8080/") {
    rds_url <- "http://host.docker.internal:8080/"
  }

  endpoint <- if (use_rawfiles) "rawfiles" else "objects"
  url <- sprintf(
    "%sstorage/projects/%s/%s/%s%s%s",
    rds_url, target_project, endpoint, folder, "%2F", file_name
  )

  resourcer::newResource(name = resource_name, url = url, format = format)
}
