generate_random_project_name <- function(skip_tests) {
  test_name <- "generate-project"
    if(any(skip_tests %in% test_name)){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

  current_projects <- armadillo.list_projects()
  random_project <- stri_rand_strings(1, 10, "[a-z0-9]")
  if (!random_project %in% current_projects) {
    return(random_project)
  } else {
    generate_random_project_name(current_projects)
  }
}
