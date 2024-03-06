add_slash_if_not_added <- function(path){
  if(!endsWith(path, "/")){
    return(paste0(path, "/"))
  } else {
    return(path)
  }
}

exit_test <- function(msg){
  cli_alert_danger(msg)
  cond = structure(list(message=msg), class=c("exit", "condition"))
  signalCondition(cond)
  stop(cond)
}

generate_random_project_name <- function() {
  current_projects <- armadillo.list_projects()
  random_project <- stri_rand_strings(1, 10, "[a-z0-9]")
  if (!random_project %in% current_projects) {
    return(random_project)
  } else {
    generate_random_project_name(current_projects)
  }
}

create_test_project <- function(target_project_name) {
    cli_alert_info(sprintf("Creating project [%s]", target_project_name))
    armadillo.create_project(target_project_name)
    cli_alert_info(sprintf("Checking if project [%s] exists", target_project_name))
    check_cohort_exists(target_project_name)
    }

check_cohort_exists <- function(cohort){
  if(cohort %in% armadillo.list_projects()){
    cli_alert_success(paste0(cohort, " exists"))
  } else {
    exit_test(paste0(cohort, " doesn't exist!"))
  }
}

create_dsi_builder <- function(server = "armadillo", url, profile, password = "", token = "", table = "", resource = "") {
  cli_alert_info("Creating new datashield login builder")
  builder <- DSI::newDSLoginBuilder()
  if (ADMIN_MODE) {
    cli_alert_info("Appending information as admin")
    builder$append(
      server = server,
      url = url,
      profile = profile,
      table = table,
      driver = "ArmadilloDriver",
      user = "admin",
      password = password,
      resource = resource
    )
  } else {
    cli_alert_info("Appending information using token")
    builder$append(
      server = server,
      url = url,
      profile = profile,
      table = table,
      driver = "ArmadilloDriver",
      token = token,
      resource = resource
    )
  }
  cli_alert_info("Appending information to login builder")
  return(builder$build())
}