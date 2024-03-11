# get request to armadillo api without authentication
get_from_api <- function(endpoint) {
  cli_alert_info(sprintf("Retrieving [%s%s]", armadillo_url, endpoint))
  response <- GET(paste0(armadillo_url, endpoint))
  cat(paste0('get_from_api', ' for ', endpoint, " results ", response$status_code, "\n"))
  return(content(response))
}

set_admin_or_get_token <- function(skip_tests, url) {
    test_name <- "set-admin-mode"
    if(skip_tests %in% test_name){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

    if(ADMIN_MODE){
      token <- admin_pwd
      auth_type <- "basic"
    } else {
      cli_alert_info("Obtaining TOKEN from '.env.")
      token <- Sys.getenv("TOKEN")
      if(token == ""){
        cli_alert_warning("TOKEN not set, obtaining from armadillo.")
        token <- armadillo.get_token(url)
      }
      auth_type <- "bearer"
    }
    return(list(token = token, auth_type = auth_type))
    cli_alert_success("Permissions set")
    }
