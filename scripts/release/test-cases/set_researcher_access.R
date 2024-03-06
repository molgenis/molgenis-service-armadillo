# # add/edit user using armadillo api
set_user <- function(user, admin_pwd, isAdmin, required_projects) {
  args <- list(email = user, admin = isAdmin, projects = required_projects)
  response <- put_to_api("access/users", admin_pwd, "basic", args)
  if(response$status_code != 204) {
    cli_alert_warning("Altering OIDC user failed, please do this manually")
    update_auto = ""
  }
}

# # armadillo api put request
put_to_api <- function(endpoint, key, auth_type, body_args){
  auth_header <- get_auth_header(auth_type, key)
  body <- jsonlite::toJSON(body_args, auto_unbox=TRUE)
  response <- PUT(paste0(armadillo_url, endpoint), body=body, encode="json",
                  config = c(httr::content_type_json(), httr::add_headers(auth_header)))
  return(response)
}

create_basic_header <- function(pwd){
  encoded <- base64enc::base64encode(
    charToRaw(
      paste0("admin:", pwd))
  )
  return(paste0("Basic ", encoded))
}

set_researcher_access <- function(required_projects) {
    if(!ADMIN_MODE){
      update_auto = "y"
      if(interactive) {
        cat("\nDo you want to remove admin from OIDC user automatically? (y/n) ")
        update_auto <- readLines("stdin", n=1)
      }
      if(update_auto == "y"){
        set_user(user, admin_pwd, F, list(required_projects))
      }
      if(update_auto != "y"){
        cat("\nGo to the Users tab")
        cat(sprintf("\nAdd [%s]' and [%s] to the project column for your account", unlist(required_projects)))
        cat("\nRevoke your admin permisions\n")
        cli_alert_warning("Make sure you either have the basic auth admin password or someone available to give you back your permissions")
        wait_for_input()
      }
      }
}
