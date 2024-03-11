create_test_project <- function(target_project_name, skip_tests) {
    test_name <- "create-test-project"
    if(skip_tests %in% test_name){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

    cli_alert_info(sprintf("Creating project [%s]", target_project_name))
    armadillo.create_project(target_project_name)
    cli_alert_info(sprintf("Checking if project [%s] exists", target_project_name))
    check_cohort_exists(target_project_name)
    }
