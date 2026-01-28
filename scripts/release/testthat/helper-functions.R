# helper-functions.R - Utility functions for testthat release tests
#
# These functions are ported from lib/common-functions.R and adapted for testthat.

# -----------------------------------------------------------------------------
# API Helper Functions
# -----------------------------------------------------------------------------

#' Create Basic authentication header
#'
#' @param pwd Admin password
#' @return Authorization header string
create_basic_header <- function(pwd) {
  encoded <- base64enc::base64encode(
    charToRaw(paste0("admin:", pwd))
  )
  return(paste0("Basic ", encoded))
}

#' Create Bearer authentication header
#'
#' @param token OAuth token
#' @return Authorization header string
create_bearer_header <- function(token) {
  return(paste0("Bearer ", token))
}

#' Get authentication header based on type
#'
#' @param type "basic" or "bearer"
#' @param key Password or token
#' @return Named vector with Authorization header
get_auth_header <- function(type, key) {
  header_content <- ""
  if (tolower(type) == "bearer") {
    header_content <- create_bearer_header(key)
  } else if (tolower(type) == "basic") {
    header_content <- create_basic_header(key)
  } else {
    stop(sprintf("Type [%s] invalid, choose from 'basic' and 'bearer'", type))
  }
  return(c("Authorization" = header_content))
}

#' PUT request to Armadillo API
#'
#' @param endpoint API endpoint
#' @param key Password or token
#' @param auth_type "basic" or "bearer"
#' @param body_args List of body arguments
#' @param url Armadillo URL
#' @return HTTP response
put_to_api <- function(endpoint, key, auth_type, body_args, url) {
  auth_header <- get_auth_header(auth_type, key)
  body <- jsonlite::toJSON(body_args, auto_unbox = TRUE)
  response <- httr::PUT(
    paste0(url, endpoint),
    body = body,
    encode = "json",
    config = c(httr::content_type_json(), httr::add_headers(auth_header))
  )
  return(response)
}

# -----------------------------------------------------------------------------
# User Management Functions
# -----------------------------------------------------------------------------

#' Set user permissions via Armadillo API
#'
#' @param user User email
#' @param admin_pwd Admin password
#' @param isAdmin Whether user should be admin
#' @param required_projects List of projects user needs access to
#' @param url Armadillo URL
set_user <- function(user, admin_pwd, isAdmin, required_projects, url) {
  args <- list(email = user, admin = isAdmin, projects = required_projects)
  response <- put_to_api("access/users", admin_pwd, "basic", args, url)
  if (response$status_code != 204) {
    cli::cli_alert_warning("Altering OIDC user failed, please do this manually") # Keep as warning - always show
  }
}

#' Set data manager permissions
#'
#' @param user User email
#' @param admin_pwd Admin password
#' @param required_projects List of required projects
#' @param interactive Whether running interactively
#' @param update_auto Auto-update setting
#' @param url Armadillo URL
set_dm_permissions <- function(user, admin_pwd, required_projects, interactive, update_auto, url) {
  if (update_auto == "y") {
    set_user(user, admin_pwd, TRUE, required_projects, url)
    cli_verbose_info("Admin reset")
  } else if (interactive) {
    cli_verbose_info("Make your account admin again")
    cat("\nPress any key to continue")
    readLines("stdin", n = 1)
  }
}

# -----------------------------------------------------------------------------
# DSI Connection Helpers
# -----------------------------------------------------------------------------

#' Create DSI login builder
#'
#' @param server Server name
#' @param url Armadillo URL
#' @param profile Profile name
#' @param password Admin password (for basic auth)
#' @param token OAuth token (for bearer auth)
#' @param table Table path
#' @param resource Resource path
#' @param ADMIN_MODE Whether using admin mode
#' @return DSI login data
create_dsi_builder <- function(server = "armadillo", url, profile, password = "",
                               token = "", table = "", resource = "", ADMIN_MODE) {
  builder <- DSI::newDSLoginBuilder()

  if (ADMIN_MODE) {
    builder$append(
      server = server,
      url = url,
      profile = profile,
      table = table,
      driver = "ArmadilloDriver",
      user = "admin",
      password = password,
      resource = resource
    )
  } else {
    builder$append(
      server = server,
      url = url,
      profile = profile,
      table = table,
      driver = "ArmadilloDriver",
      token = token,
      resource = resource
    )
  }

  return(builder$build())
}

# -----------------------------------------------------------------------------
# Resource Management Functions
# -----------------------------------------------------------------------------

#' Download multiple resource sources
#'
#' @param ref Reference tibble with path and url columns
download_many_sources <- function(ref) {
  purrr::pmap(ref, function(path, url, ...) {
    if (!file.exists(path)) {
      cli_verbose_info(sprintf("Downloading %s...", basename(path)))
      download.file(url, path, quiet = TRUE)
    }
  })
}

#' Upload multiple resource sources
#'
#' @param project Project name
#' @param ref Reference tibble
#' @param url Armadillo URL
#' @param token OAuth token
#' @param auth_type Authentication type
#' @param folder Target folder
upload_many_sources <- function(project, ref, url, token, auth_type, folder) {
  source(file.path(test_env$test_cases_dir, "upload-resource.R"))
  purrr::pmap(ref, function(path, file_name, ...) {
    upload_resource(
      project = project,
      rda_dir = path,
      url = url,
      token = token,
      folder = folder,
      file_name = file_name,
      auth_type = auth_type,
      skip_tests = NULL
    )
  })
}

#' Create multiple resources
#'
#' @param ref Reference tibble
#' @param project Project name
#' @param folder Target folder
#' @param url Armadillo URL
#' @return List of created resources
create_many_resources <- function(ref, project, folder, url) {
  source(file.path(test_env$test_cases_dir, "create-resource.R"))
  purrr::pmap(ref, function(object_name, format, file_name, ...) {
    create_resource(
      target_project = project,
      url = url,
      folder = folder,
      format = format,
      file_name = file_name,
      resource_name = object_name,
      skip_tests = NULL
    )
  })
}

#' Upload multiple resources to Armadillo
#'
#' @param project Project name
#' @param resource List of resource objects
#' @param folder Target folder
#' @param ref Reference tibble
upload_many_resources <- function(project, resource, folder, ref) {
  list(resource = resource, name = ref$object_name) %>%
    purrr::pmap(function(resource, name) {
      MolgenisArmadillo::armadillo.upload_resource(
        project = project,
        folder = folder,
        resource = resource,
        name = name
      )
    })
}

#' Assign multiple resources via DataSHIELD
#'
#' @param project Project name
#' @param folder Folder name
#' @param ref Reference tibble
#' @param conns DataSHIELD connections
#' @note suppressMessages hides "Data in all studies were valid" which is expected behavior
assign_many_resources <- function(project, folder, ref, conns) {
  purrr::map(ref$object_name, function(x) {
    exp_resource_path <- paste0(project, "/", folder, "/", x)
    suppressMessages(DSI::datashield.assign.resource(conns, resource = exp_resource_path, symbol = x))
  })
}

#' Resolve multiple resources as data frames
#'
#' @param resource_names Vector of resource names
#' @param conns DataSHIELD connections
#' @note suppressMessages hides "Data in all studies were valid" which is expected behavior
resolve_many_resources <- function(resource_names, conns) {
  purrr::map(resource_names, function(x) {
    suppressMessages(DSI::datashield.assign.expr(
      conns,
      symbol = x,
      expr = as.symbol(paste0("as.resource.data.frame(", x, ")"))
    ))
  })
}
