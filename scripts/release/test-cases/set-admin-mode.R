set_admin_or_get_token <- function(admin_pwd, url, skip_tests, ADMIN_MODE) {
    test_name <- "set-admin-mode"
    if(skip_tests %in% test_name){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

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
    return(list(token = token))
    cli_alert_success("Permissions set")
    }
