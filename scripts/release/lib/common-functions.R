add_slash_if_not_added <- function(path) {
  if (!endsWith(path, "/")) {
    return(paste0(path, "/"))
  } else {
    return(path)
  }
}

remove_slash_if_added <- function(path) {
  if (endsWith(path, "/")) {
    return(gsub("/$", "", path))
  } else {
    return(path)
  }
}

exit_test <- function(msg) {
  cli_alert_danger(msg)
  if (testthat::is_testing()) {
    fail(msg)
  } else {
    stop(msg, call. = FALSE)
  }
}

check_cohort_exists <- function(cohort) {
  if (cohort %in% armadillo.list_projects()) {
    cli_alert_success(paste0(cohort, " exists"))
  } else {
    exit_test(paste0(cohort, " doesn't exist!"))
  }
}

wait_for_input <- function(interactive) {
  if (interactive) {
    cat("\nPress any key to continue")
    continue <- readLines("stdin", n = 1)
  } else {
    cat("\n\n")
  }
}

create_basic_header <- function(pwd) {
  encoded <- base64enc::base64encode(
    charToRaw(
      paste0("admin:", pwd)
    )
  )
  return(paste0("Basic ", encoded))
}

# # add/edit user using armadillo api
set_user <- function(isAdmin, required_projects) {
  args <- list(email = release_env$user, admin = isAdmin, projects = required_projects)
  response <- put_to_api("access/users", release_env$admin_pwd, "basic", args, release_env$armadillo_url)
  if (response$status_code != 204) {
    cli_alert_warning("Altering OIDC user failed, please do this manually")
  }
}

# # armadillo api put request
put_to_api <- function(endpoint, key, auth_type, body_args, url) {
  auth_header <- get_auth_header(auth_type, key)
  body <- jsonlite::toJSON(body_args, auto_unbox = TRUE)
  response <- PUT(paste0(url, endpoint),
    body = body, encode = "json",
    config = c(httr::content_type_json(), httr::add_headers(auth_header))
  )
  return(response)
}

should_skip_test <- function(test_name) {
  any(release_env$skip_tests %in% test_name)
}

do_skip_test <- function(test_name) {
  if (should_skip_test(test_name)) {
    testthat::skip(sprintf("Test '%s' skipped", test_name))
  }
}

read_parquet_with_message <- function(file_path, dest) {
  cli_alert_info(file_path)
  out <- arrow::read_parquet(paste0(dest, paste0(file_path, ".parquet")))
  cli_alert_success(paste0(file_path, " read"))
  return(out)
}

run_spinner <- function(spinner) {
  lapply(1:1000, function(x) {
    spinner$spin()
    spin_till_done(spinner)
  })
}

# # compare values in two lists
compare_list_values <- function(list1, list2) {
  vals_to_print <- cli_ul()
  equal <- TRUE
  for (i in 1:length(list1)) {
    val1 <- list1[i]
    val2 <- list2[i]
    if (almost_equal(val1, val2) == TRUE) {
      cli_li(sprintf("%s ~= %s", val1, val2))
    } else {
      equal <- FALSE
      cli_li(sprintf("%s != %s", val1, val2))
    }
  }
  cli_end(vals_to_print)
  if (equal) {
    cli_alert_success("Values equal")
  } else {
    cli_alert_danger("Values not equal")
  }
}

# theres a bit of noise added in DataSHIELD answers, causing calculations to not always be exactly the same, but close
# here we check if they're equal enough
almost_equal <- function(val1, val2) {
  return(all.equal(val1, val2, tolerance = .Machine$double.eps^0.03))
}

generate_random_project_name <- function() {
  current_projects <- armadillo.list_projects()
  random_project <- stri_rand_strings(1, 10, "[a-z0-9]")
  if (!random_project %in% current_projects) {
    cli_alert_success(sprintf("Project %s generated", random_project))
    return(random_project)
  } else {
    generate_random_project_name()
  }
}

get_from_api <- function(endpoint, armadillo_url) {
  cli_alert_info(sprintf("Retrieving [%s%s]", armadillo_url, endpoint))
  response <- GET(paste0(armadillo_url, endpoint))
  cat(paste0("get_from_api", " for ", endpoint, " results ", response$status_code, "\n"))
  return(content(response))
}

get_auth_type <- function(ADMIN_MODE) {
  if (ADMIN_MODE) {
    auth_type <- "basic"
  } else {
    auth_type <- "bearer"
  }
}

# get request to armadillo api with an authheader
get_from_api_with_header <- function(endpoint, key, auth_type, url, user) {
  auth_header <- get_auth_header(auth_type, key)
  response <- GET(paste0(url, endpoint), config = c(httr::add_headers(auth_header)))
  if (response$status_code == 403) {
    msg <- sprintf("Permission denied. Is user [%s] admin?", user)
    exit_test(msg)
  } else if (response$status_code != 200) {
    cli_alert_danger(sprintf("Cannot retrieve data from endpoint [%s]", endpoint))
    exit_test(content(response)$message)
  }
  return(content(response))
}

# make authentication header for api calls, basic or bearer based on type
get_auth_header <- function(type, key) {
  header_content <- ""
  if (tolower(type) == "bearer") {
    header_content <- create_bearer_header(key)
  } else if (tolower(type) == "basic") {
    header_content <- create_basic_header(key)
  } else {
    exit_test(sprintf("Type [%s] invalid, choose from 'basic' and 'bearer'"))
  }
  return(c("Authorization" = header_content))
}

create_bearer_header <- function(token) {
  return(paste0("Bearer ", token))
}

print_list <- function(list) {
  vals_to_print <- cli_ul()
  for (i in 1:length(list)) {
    val <- list[i]
    cli_li(val)
  }
  cli_end(vals_to_print)
}

set_dm_permissions <- function() {
  if (release_env$update_auto == "y") {
    set_user(T, list(release_env$project1))
    cli_alert_info("Admin reset")
  } else {
    cli_alert_info("Make your account admin again")
    wait_for_input(release_env$interactive)
  }
}

download_many_sources <- function(ref) {
  ref %>%
    pmap(function(path, url, ...) {
      prepare_resources(resource_path = path, url = url)
    })
}

upload_many_sources <- function(ref, folder) {
  ref %>%
    pmap(function(path, file_name, ...) {
      upload_resource(folder = folder, file_name = file_name, rda_dir = path)
    })
}

create_many_resources <- function(ref, folder) {
  ref %>%
    pmap(function(object_name, format, file_name, ...) {
      create_resource(folder = folder, format = format, file_name = file_name, resource_name = object_name)
    })
}

upload_many_resources <- function(resource, folder, ref) {
  list(resource = resource, name = ref$object_name) %>%
    pmap(function(resource, name) {
      armadillo.upload_resource(project = release_env$project1, folder = folder, resource = resource, name = name)
    })
}

assign_many_resources <- function(folder, ref) {
  ref$object_name %>%
    map(function(x) {
      exp_resource_path <- paste0(release_env$project1, "/", folder, "/", x)
      datashield.assign.resource(release_env$conns, resource = exp_resource_path, symbol = x)
    })
}

resolve_many_resources <- function(resource_names) {
  resource_names %>%
    map(~ datashield.assign.expr(release_env$conns, symbol = .x, expr = as.symbol(paste0("as.resource.data.frame(", .x, ")"))))
}

