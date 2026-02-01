# post resource to armadillo api
post_resource_to_api <- function(project, key, auth_type, file, folder, name, url) {
  auth_header <- get_auth_header(auth_type, key)
  plan(multisession)
  spinner <- make_spinner()
  # Do async call
  api_call <- future(POST(sprintf("%sstorage/projects/%s/objects", url, project),
    body = list(file = file, object = paste0(folder, "/", name)),
    config = c(httr::add_headers(auth_header))
  ))

  # Run spinner while waiting for response
  # Response will come when ready
  response <- value(api_call)
  # Set do_run_spinner to false, causing the spinner to stop running, see spin_till_done method
  do_run_spinner <- FALSE
  if (response$status_code != 204) {
    cli_alert_warning(sprintf("Could not upload [%s] to project [%s]", name, project))
    exit_test(content(response)$message)
  }
}

spin_till_done <- function(spinner) {
  # run_spinner is a boolean set on top of this script, it is set to false when loading is done and spinner can stop
  do_run_spinner <- TRUE
  if (do_run_spinner) {
    Sys.sleep(0.1)
  } else {
    spinner$finish()
  }
}

upload_resource <- function(folder, file_name, rda_dir = release_env$rda_dir) {
  test_name <- "upload-resource"
  if (do_skip_test(test_name)) {
    return()
  }
  rda_file_body <- upload_file(rda_dir)
  cli_alert_info(sprintf("Uploading resource file to %s into project [%s]", release_env$armadillo_url, release_env$project1))
  system.time({
    post_resource_to_api(release_env$project1, release_env$token, release_env$auth_type, rda_file_body, folder, file_name, release_env$armadillo_url)
  })
  cli_alert_success(sprintf("%s passed!", test_name))
}
