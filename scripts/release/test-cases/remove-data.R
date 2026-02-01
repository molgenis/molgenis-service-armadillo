library(purrr)

dm_delete_tables <- function() {
  armadillo.delete_table(release_env$project1, "2_1-core-1_0", "nonrep")
  armadillo.delete_table(release_env$project1, "2_1-core-1_0", "yearlyrep")
  armadillo.delete_table(release_env$project1, "2_1-core-1_0", "trimesterrep")
  armadillo.delete_table(release_env$project1, "2_1-core-1_0", "monthlyrep")
  armadillo.delete_table(release_env$project1, "1_1-outcome-1_0", "nonrep")
  armadillo.delete_table(release_env$project1, "1_1-outcome-1_0", "yearlyrep")
}

dm_delete_projects <- function(project) {
  cat(sprintf("\nVerify in UI all data from [%s] is gone.", project))
  wait_for_input(release_env$interactive)
  armadillo.delete_project(project)
  wait_for_input(release_env$interactive)
}

dm_clean_up <- function() {
  test_name <- "remove-data"
  if (do_skip_test(test_name)) {
    return()
  }

  cli_alert_info("We're now continuing with the datamanager workflow as admin\n")
  cli_alert_info("Resetting admin permissions")
  set_dm_permissions(list(release_env$project1))
  cli_alert_info("Removing tables")
  dm_delete_tables()
  cli_alert_info("Removing projects")
  map(list(release_env$project1), ~ dm_delete_projects(.x))
  cli_alert_success(sprintf("%s passed!", test_name))
}
