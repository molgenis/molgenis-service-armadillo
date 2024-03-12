add_slash_if_not_added <- function(path) {
  if(!endsWith(path, "/")){
    return(paste0(path, "/"))
  } else {
    return(path)
  }
}

exit_test <- function(msg) {
  cli_alert_danger(msg)
  cond = structure(list(message=msg), class=c("exit", "condition"))
  signalCondition(cond)
  stop(cond)
}

generate_random_project_name <- function() {
  current_projects <- armadillo.list_projects()
  random_project <- stri_rand_strings(1, 10, "[a-z0-9]")
  if (!random_project %in% current_projects) {
    return(random_project)
  } else {
    generate_random_project_name(current_projects)
  }
}

check_cohort_exists <- function(cohort) {
  if(cohort %in% armadillo.list_projects()){
    cli_alert_success(paste0(cohort, " exists"))
  } else {
    exit_test(paste0(cohort, " doesn't exist!"))
  }
}

wait_for_input <- function(interactive) {
  if (interactive) {
    cat("\nPress any key to continue")
    continue <- readLines("stdin", n=1)
  }
  else {
    cat("\n\n")
  }
}

create_basic_header <- function(pwd) {
  encoded <- base64enc::base64encode(
    charToRaw(
      paste0("admin:", pwd))
  )
  return(paste0("Basic ", encoded))
}

# # add/edit user using armadillo api
set_user <- function(user, admin_pwd, isAdmin, required_projects, url) {
  args <- list(email = user, admin = isAdmin, projects = required_projects)
  response <- put_to_api("access/users", admin_pwd, "basic", args, url)
  if(response$status_code != 204) {
    cli_alert_warning("Altering OIDC user failed, please do this manually")
    update_auto = ""
  }
}

# FUNCTIONS BELOW IN MAIN SCRIPT BUT DON'T APPEAR TO BE CALLED
#
# create_dir_if_not_exists <- function(directory){
#   if (!dir.exists(paste0(dest, directory))) {
#     dir.create(paste0(dest, directory))
#   }
# }
#
# download_test_files <- function(urls, dest){
#   n_files <- length(urls)
#   cli_progress_bar("Downloading testfiles", total = n_files)
#   for (i in 1:n_files) {
#     download_url <- urls[i]
#     splitted <- strsplit(download_url, "/")[[1]]
#     folder <- splitted[length(splitted) - 1]
#     filename <- splitted[length(splitted)]
#     cli_alert_info(paste0("Downloading ", filename))
#     download.file(download_url, paste0(dest, folder, "/", filename), quiet=TRUE)
#     cli_progress_update()
#   }
#   cli_progress_done()
# }
#
# generate_random_project_seed <- function(current_project_seeds) {
#   random_seed <- round(runif(1, min = 100000000, max=999999999))
#   if (!random_seed %in% current_project_seeds) {
#     return(random_seed)
#   } else {
#     generate_random_project_seed(current_project_seeds)
#   }
# }
#
# generate_project_port <- function(current_project_ports) {
#   starting_port <- 6312
#   while (starting_port %in% current_project_ports) {
#     starting_port = starting_port + 1
#   }
#   return(starting_port)
# }
#
# obtain_existing_profile_information <- function(key, auth_type) {
#   responses <- get_from_api_with_header('ds-profiles', key, auth_type)
#   response_df <- data.frame(matrix(ncol=5,nrow=0, dimnames=list(NULL, c("name", "container", "port", "seed", "online"))))
#   for (response in responses) {
#     if("datashield.seed" %in% names(response$options)) {
#       datashield_seed <- response$options$datashield.seed
#     } else {
#       datashield_seed <- NA
#     }
#
#     response_df[nrow(response_df) + 1,] = c(response$name, response$image, response$port, datashield_seed, response$container$status)
#   }
#   return(response_df)
# }
#

#
# return_list_without_empty <- function(to_empty_list) {
#   return(to_empty_list[to_empty_list != ''])
# }
#
# create_profile <- function(profile_name, key, auth_type) {
#   if (profile_name %in% profile_defaults$name) {
#     cli_alert_info(sprintf("Creating profile: %s", profile_name))
#     profile_default <- profile_defaults[profile_defaults$name == profile_name,]
#     current_profiles <- obtain_existing_profile_information(key, auth_type)
#     new_profile_seed <- generate_random_project_seed(current_profiles$seed)
#     whitelist <- as.list(stri_split_fixed(paste("dsBase", profile_default$whitelist, sep = ","), ",")[[1]])
#     blacklist <- as.list(stri_split_fixed(profile_default$blacklist, ",")[[1]])
#     port <- profile_default$port
#     if (port == "") {
#       port <- generate_project_port(current_profiles$port)
#     }
#     args <- list(
#       name = profile_name,
#       image = profile_default$container,
#       host = "localhost",
#       port = port,
#       packageWhitelist = return_list_without_empty(whitelist),
#       functionBlacklist = return_list_without_empty(blacklist),
#       options = list(datashield.seed = new_profile_seed)
#     )
#     response <- put_to_api('ds-profiles', key, auth_type, body_args = args)
#     if (response$status_code == 204) {
#       cli_alert_success(sprintf("Profile %s successfully created.", profile_name))
#       start_profile(profile_name, key, auth_type)
#     } else {
#       exit_test(sprintf("Unable to create profile: %s , errored %s", profile_name, response$status_code))
#     }
#   } else {
#     exit_test(sprintf("Unable to create profile: %s , unknown profile", profile_name))
#   }
# }
#