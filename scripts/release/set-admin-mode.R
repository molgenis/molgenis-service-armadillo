# get request to armadillo api without authentication
get_from_api <- function(endpoint) {
  cli_alert_info(sprintf("Retrieving [%s%s]", armadillo_url, endpoint))
  response <- GET(paste0(armadillo_url, endpoint))
  cat(paste0('get_from_api', ' for ', endpoint, " results ", response$status_code, "\n"))
  return(content(response))
}

app_info <- get_from_api("actuator/info")

version <- unlist(app_info$build$version)

if(ADMIN_MODE){
  token <- admin_pwd
  auth_type <- "basic"
} else {
  cli_alert_info("Obtaining TOKEN from '.env.")
  token <- Sys.getenv("TOKEN")
  if(token == ""){
    cli_alert_warning("TOKEN not set, obtaining from armadillo.")
    token <- armadillo.get_token(armadillo_url)
  }
  auth_type <- "bearer"
}
