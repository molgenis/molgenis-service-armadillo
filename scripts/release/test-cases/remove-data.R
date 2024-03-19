library(purrr)

set_dm_permissions <- function(user, admin_pwd, required_projects, interactive, update_auto, url) {
  if (update_auto == "y") {
    set_user(user, admin_pwd, T, required_projects, url)
    cli_alert_info("Admin reset")
  } else {
    cli_alert_info("Make your account admin again")
    wait_for_input(interactive)
  }
}

dm_delete_tables <- function() {
  armadillo.delete_table(project1, "2_1-core-1_0", "nonrep")
  armadillo.delete_table(project1, "2_1-core-1_0", "yearlyrep")
  armadillo.delete_table(project1, "2_1-core-1_0", "trimesterrep")
  armadillo.delete_table(project1, "2_1-core-1_0", "monthlyrep")
  armadillo.delete_table(project1, "1_1-outcome-1_0", "nonrep")
  armadillo.delete_table(project1, "1_1-outcome-1_0", "yearlyrep")
}

dm_delete_projects <- function(project, interactive) {
  cat(sprintf("\nVerify in UI all data from [%s] is gone.", project))
  wait_for_input(interactive)
  armadillo.delete_project(project)
  wait_for_input(interactive)
}

dm_clean_up <- function(user, admin_pwd, required_projects, interactive, update_auto, url, skip_tests) {
  test_name <- "remove-data"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }

  cli_alert_info("We're now continuing with the datamanager workflow as admin\n")
  cli_alert_info("Resetting admin permissions")
  set_dm_permissions(user, admin_pwd, required_projects, interactive, update_auto, url)
  cli_alert_info("Removing tables")
  dm_delete_tables()
  cli_alert_info("Removing projects")
  map(required_projects, ~ dm_delete_projects(.x, interactive))
  cli_alert_success(sprintf("%s passed!", test_name))
}
