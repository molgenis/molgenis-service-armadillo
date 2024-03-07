# post resource to armadillo api
post_resource_to_api <- function(project, key, auth_type, file, folder, name) {
  auth_header <- get_auth_header(auth_type, key)
  plan(multisession)
  spinner <- make_spinner()
  # Do async call
  api_call <- future(POST(sprintf("%sstorage/projects/%s/objects", armadillo_url, project),
    body=list(file = file, object=paste0(folder,"/", name)),
                    config = c(httr::add_headers(auth_header))))

  # Run spinner while waiting for response
  ansi_with_hidden_cursor(run_spinner(spinner))
  # Response will come when ready
  response <- value(api_call)
  # Set do_run_spinner to false, causing the spinner to stop running, see spin_till_done method
  do_run_spinner <- FALSE
  if(response$status_code != 204) {
    cli_alert_warning(sprintf("Could not upload [%s] to project [%s]", name, project))
    exit_test(content(response)$message)
  }
}

run_spinner <- function(spinner) {
  lapply(1:1000, function(x) { spinner$spin(); spin_till_done(spinner)})
}

# omics_project <- generate_random_project_name(available_projects)
# available_projects <- c(available_projects, omics_project)

# cli_alert_info(sprintf("Creating project [%s]", omics_project))
# armadillo.create_project(omics_project)

spin_till_done <- function(spinner) {
    # run_spinner is a boolean set on top of this script, it is set to false when loading is done and spinner can stop
    if (do_run_spinner) {
        Sys.sleep(0.1)
    } else {
        spinner$finish()
    }
}

upload_resource <- function(project, rda_dir, url, token, auth_type) {
    rda_file_body <- upload_file(rda_dir)
    cli_alert_info(sprintf("Uploading resource file to %s into project [%s]", url, project))
    system.time({
      post_resource_to_api(project, token, auth_type, rda_file_body, "ewas", "gse66351_1.rda")
    })
    cli_alert_info("Resource uploaded")
    }
