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

# Get the path where table files are located (checks all required files)
get_tables_path <- function() {
  table_files <- c(
    "core/nonrep.parquet",
    "core/yearlyrep.parquet",
    "core/monthlyrep.parquet",
    "core/trimesterrep.parquet",
    "outcome/nonrep.parquet",
    "outcome/yearlyrep.parquet",
    "survival/veteran.parquet"
  )

  # Check default path first
  default_paths <- file.path(release_env$default_parquet_path, table_files)
  if (all(file.exists(default_paths))) {
    return(release_env$default_parquet_path)
  }

  # Fall back to dest path
  return(release_env$dest)
}

# Download file with cli progress bar using curl
download_with_progress <- function(url, destfile) {
  id <- NULL

  h <- curl::new_handle()
  curl::handle_setopt(h, noprogress = FALSE, progressfunction = function(down, up) {
    # down[1] = total to download, down[2] = downloaded so far
    if (down[1] > 0) {
      if (is.null(id)) {
        id <<- cli_progress_bar(
          format = "{cli::pb_bar} {cli::pb_percent} | {cli::pb_current_bytes}/{cli::pb_total_bytes}",
          total = down[1],
          clear = FALSE
        )
      }
      cli_progress_update(id = id, set = down[2])
    }
    TRUE
  })

  curl::curl_download(url, destfile, handle = h, quiet = TRUE)

  if (!is.null(id)) {
    cli_progress_done(id = id)
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

# Add/edit user using armadillo api
set_user <- function(isAdmin, required_projects) {
  args <- list(email = release_env$user, admin = isAdmin, projects = required_projects)
  response <- put_to_api("access/users", release_env$admin_pwd, "basic", args, release_env$armadillo_url)
  if (response$status_code != 204) {
    cli_alert_warning("Altering OIDC user failed, please do this manually")
  }
}

# Armadillo api put request
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
  test_name %in% release_env$skip_tests
}

do_skip_test <- function(test_name) {
  if (should_skip_test(test_name)) {
    testthat::skip(sprintf("Test '%s' skipped", test_name))
  }
}

skip_if_no_resources <- function(test_name) {
  do_skip_test(test_name)
  # TODO: re-enable once resource tests work in admin mode
  # testthat::skip_if(release_env$ADMIN_MODE, "Cannot test resources as admin")
  testthat::skip_if(!"resourcer" %in% release_env$container_info$packageWhitelist,
                    sprintf("resourcer not available for container: %s", release_env$current_container))
}

skip_if_localhosts <- function(url, test_name) {
  do_skip_test(test_name)
  # TODO: re-enable when version number can be reliably retrieved over localhost
  testthat::skip_if(!"localhost" %in% url,
                    sprintf("version cannot be retrieved locally"))
}

read_parquet_with_message <- function(file_path, dest) {
  cli_progress_step(sprintf("Reading %s", file_path))
  out <- arrow::read_parquet(paste0(dest, paste0(file_path, ".parquet")))
  cli_progress_done()
  return(out)
}

# Check if values are approximately equal (DataSHIELD adds noise to results)
almost_equal <- function(val1, val2) {
  return(all.equal(val1, val2, tolerance = .Machine$double.eps^0.03))
}

generate_random_project_name <- function() {
  # Generate random project name without checking existing projects
  # to avoid requiring authentication at config time.
  # With 36^10 possible combinations, collision risk is ~0%
  random_project <- stri_rand_strings(1, 10, "[a-z0-9]")
  return(random_project)
}

get_auth_type <- function(ADMIN_MODE) {
  if (ADMIN_MODE) {
    return("basic")
  } else {
    return("bearer")
  }
}

# Get request to armadillo api with an auth header
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

# Make authentication header for api calls, basic or bearer based on type
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
  missing <- ref[!file.exists(ref$path), ]
  if (nrow(missing) > 0) {
    cli_alert_warning(sprintf("Missing %d resource(s)", nrow(missing)))
    cli_alert_info(sprintf("Downloading into: %s", release_env$test_file_path))
    for (i in seq_len(nrow(missing))) {
      row <- missing[i, ]
      cli_alert_info(sprintf("Downloading %s (%d/%d) from: %s", row$file_name, i, nrow(missing), row$url))
      download_with_progress(row$url, row$path)
    }
    cli_alert_success("Test resources downloaded")
  } else {
    cli_alert_success("Test resources available locally")
  }
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

create_dsi_builder <- function(server = "armadillo", table = "", resource = "") {
  builder <- DSI::newDSLoginBuilder()
  if (release_env$ADMIN_MODE) {
    builder$append(
      server = server,
      url = release_env$armadillo_url,
      profile = release_env$current_container,
      table = table,
      driver = "ArmadilloDriver",
      user = "admin",
      password = release_env$admin_pwd,
      resource = resource
    )
  } else {
    builder$append(
      server = server,
      url = release_env$armadillo_url,
      profile = release_env$current_container,
      table = table,
      driver = "ArmadilloDriver",
      token = release_env$token,
      resource = resource
    )
  }
  return(builder$build())
}

resolve_many_resources <- function(resource_names) {
  resource_names %>%
    map(~ datashield.assign.expr(release_env$conns, symbol = .x, expr = as.symbol(paste0("as.resource.data.frame(", .x, ")"))))
}
