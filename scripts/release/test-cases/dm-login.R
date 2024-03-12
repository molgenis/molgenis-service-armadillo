dm_login <- function(url, ADMIN_MODE, admin_pwd, skip_tests) {
    test_name <- "dm_login"
    if(any(skip_tests %in% test_name)){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

    cli_alert_info(sprintf("Login to %s", url))
    if(test_config$ADMIN_MODE) {
        armadillo.login_basic(url, "admin", test_config$admin_pwd)
    } else {
        armadillo.login(url)
    }
    cli_alert_success("Logged in")
    }
