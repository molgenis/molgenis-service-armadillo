# Post resource to armadillo api
post_resource_to_api <- function(project, key, auth_type, file, folder, name, url) {
  auth_header <- get_auth_header(auth_type, key)
  response <- POST(sprintf("%sstorage/projects/%s/objects", url, project),
    body = list(file = file, object = paste0(folder, "/", name)),
    config = c(httr::add_headers(auth_header))
  )
  if (response$status_code != 204) {
    cli_alert_warning(sprintf("Could not upload [%s] to project [%s]", name, project))
    exit_test(content(response)$message)
  }
}

upload_resource <- function(folder, file_name, rda_dir = release_env$rda_dir) {
  rda_file_body <- upload_file(rda_dir)
  post_resource_to_api(release_env$project1, release_env$token, release_env$auth_type, rda_file_body, folder, file_name, release_env$armadillo_url)
}
