# # armadillo api put request
put_to_api <- function(endpoint, key, auth_type, body_args, url) {
  auth_header <- get_auth_header(auth_type, key)
  body <- jsonlite::toJSON(body_args, auto_unbox=TRUE)
  response <- PUT(paste0(url, endpoint), body=body, encode="json",
                  config = c(httr::content_type_json(), httr::add_headers(auth_header)))
  return(response)
}

set_researcher_access <- function(url, interactive, required_projects, user, admin_pwd, skip_tests) {
    test_name <- "set_researcher_access"
    if(any(skip_tests %in% test_name)){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

    if(!ADMIN_MODE){
      update_auto = "y"
      if(interactive) {
        cat("\nDo you want to remove admin from OIDC user automatically? (y/n) ")
        update_auto <- readLines("stdin", n=1)
      }
      if(update_auto == "y"){
        set_user(user, admin_pwd, F, required_projects, url)
      }
      if(update_auto != "y"){
        cat("\nGo to the Users tab")
        cat(sprintf("\nAdd [%s]' and [%s] to the project column for your account", unlist(required_projects)))
        cat("\nRevoke your admin permisions\n")
        cli_alert_warning("Make sure you either have the basic auth admin password or someone available to give you back your permissions")
        wait_for_input(interactive)
      }
      }
}
