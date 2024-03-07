library(purrr)

set_dm_permissions <- function(user, admin_pwd, required_projects) {
    if(update_auto == "y"){
      set_user(user, admin_pwd, T, required_projects)
    } else{
      cat("Make your account admin again")
      wait_for_input()
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

dm_delete_projects <- function(project) {
   cat(sprintf("\nVerify in UI all data from [%s] is gone.", project))
   wait_for_input()
   armadillo.delete_project(project)
   wait_for_input()
   }

dm_clean_up <- function(user, admin_pwd, required_projects) {
    cli_alert_info("We're now continuing with the datamanager workflow as admin\n")
    cli_alert_info("Resetting admin permissions")
    set_dm_permissions(user, admin_pwd, required_projects)
    cli_alert_info("Removing tables")
    dm_delete_tables()
    cli_alert_info("Removing projects")
    map(required_projects, dm_delete_projects)
}
