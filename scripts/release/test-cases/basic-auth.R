verify_basic_auth = function(url, admin_pwd, dest, skip_tests){
  test_name = "basic_auth"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }
  
  if(admin_pwd != "") {
  cli_h2("Basic authentication")
  cli_alert_info("Logging in as admin user")
  armadillo.login_basic(url, "admin", admin_pwd)
  project <- generate_random_project_name(skip_tests)
  print(project)
  cli_alert_info(sprintf("Creating project [%s]", project))
  armadillo.create_project(project)
  nonrep <- arrow::read_parquet(paste0(dest, "core/nonrep.parquet"))
  cli_alert_info(sprintf("Uploading file to [%s]", project))
  armadillo.upload_table(project, "2_1-core-1_0", nonrep)
  rm(nonrep)
  check_cohort_exists(project)
  table <- sprintf("%s/2_1-core-1_0/nonrep", project)
  if(table %in% armadillo.list_tables(project)){
    cli_alert_success(paste0(table, " exists"))
  } else {
    exit_test(paste0(table, " doesn't exist"))
  }
  cli_alert_info(sprintf("Deleting [%s]", project))
  armadillo.delete_project(project)
} else {
  cli_alert_warning("Testing basic authentication skipped, admin password not available")
}
  cli_alert_success(sprintf("%s passed!", test_name))
}
