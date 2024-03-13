set_admin_or_get_token <- function(admin_pwd, url, skip_tests, ADMIN_MODE) {
    test_name <- "set-admin-mode"
    if(do_skip_test(test_name, skip_tests)) {return()}

    if(ADMIN_MODE){
      token <- admin_pwd
    } else {
      cli_alert_info("Obtaining TOKEN from '.env.")
      token <- Sys.getenv("TOKEN")
      if(token == ""){
        cli_alert_warning("TOKEN not set, obtaining from armadillo.")
        token <- armadillo.get_token(url)
      }
    }
    return(token = token)
    cli_alert_success(sprintf("%s passed!", test_name))
    }
