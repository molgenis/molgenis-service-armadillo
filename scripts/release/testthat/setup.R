# setup.R - Lazy initialization for testthat release tests
#
# This file provides setup functions that use lazy initialization.
# Each ensure_* function only runs its setup once, caching results in test_env.
# Functions chain to their dependencies automatically.
#
# Dependency chains:
#
# For TABLE tests (ds-base, ds-survival, etc.):
#   ensure_config() <- ensure_tables_downloaded() <- ensure_tables_uploaded()
#                   <- ensure_researcher_login_and_assign()
#
# For RESOURCE tests (ds-exposome, ds-omics):
#   ensure_config() <- ensure_resources_downloaded() <- ensure_resources_uploaded()
#                   <- ensure_researcher_login()

# Shared test environment - persists across all test files
test_env <- new.env()

# -----------------------------------------------------------------------------
# Verbose output helpers
# -----------------------------------------------------------------------------
# These functions only output when VERBOSE=true, keeping non-verbose runs clean.
# Critical messages (errors, warnings) should use cli:: directly to always show.

cli_verbose_h1 <- function(...) {
  if (isTRUE(test_env$verbose_mode)) cli::cli_h1(...)
}

cli_verbose_h2 <- function(...) {
  if (isTRUE(test_env$verbose_mode)) cli::cli_h2(...)
}

cli_verbose_info <- function(...) {
  if (isTRUE(test_env$verbose_mode)) cli::cli_alert_info(...)
}

cli_verbose_success <- function(...) {
  if (isTRUE(test_env$verbose_mode)) cli::cli_alert_success(...)
}

cli_verbose_ul <- function(...) {
  if (isTRUE(test_env$verbose_mode)) cli::cli_ul(...)
}

cli_verbose_text <- function(...) {
  if (isTRUE(test_env$verbose_mode)) cli::cli_text(...)
}

# Store the testthat directory path for resolving source() paths
test_env$testthat_dir <- normalizePath(dirname(sys.frame(1)$ofile), mustWork = FALSE)
if (is.na(test_env$testthat_dir) || test_env$testthat_dir == "") {
  test_env$testthat_dir <- getwd()
}
test_env$test_cases_dir <- file.path(dirname(test_env$testthat_dir), "test-cases")

# -----------------------------------------------------------------------------
# Level 1: Configuration
# -----------------------------------------------------------------------------

ensure_config <- function() {
  if (!is.null(test_env$config)) {
    return(invisible(test_env$config))
  }

  cli_verbose_info("Loading test configuration...")

  # Read .env from parent of testthat directory (scripts/release/)
  env_file <- file.path(dirname(test_env$testthat_dir), ".env")
  if (file.exists(env_file)) {
    readRenviron(env_file)
  }

  skip_tests <- Sys.getenv("SKIP_TESTS")
  skip_tests <- stringr::str_split(skip_tests, ",")[[1]]

  armadillo_url <- Sys.getenv("ARMADILLO_URL")
  if (armadillo_url == "") {
    cli::cli_alert_warning("Defaulting to https://armadillo-demo.molgenis.net/")
    armadillo_url <- "https://armadillo-demo.molgenis.net/"
  }

  if (stringr::str_detect(armadillo_url, "localhost") && !any(skip_tests %in% "ds-omics")) {
    skip_tests <- c(skip_tests, "ds-omics")
  }

  interactive <- Sys.getenv("INTERACTIVE") != "N"
  armadillo_url <- add_slash_if_not_added(armadillo_url)

  if (!RCurl::url.exists(armadillo_url)) {
    cli::cli_alert_danger(sprintf("URL [%s] is not reachable!", armadillo_url))
    cli::cli_alert_info("Please check:")
    cli::cli_ul(c(
      "Is Armadillo running?",
      "Is the URL correct in .env file?",
      "Are you connected to the network/VPN?"
    ))
    stop(sprintf("URL [%s] doesn't exist", armadillo_url))
  }
  cli_verbose_success(sprintf("Armadillo URL reachable: %s", armadillo_url))

  if (!startsWith(armadillo_url, "http")) {
    armadillo_url <- paste0(if (startsWith(armadillo_url, "localhost")) "http://" else "https://", armadillo_url)
  }

  as_docker_container <- Sys.getenv("AS_DOCKER_CONTAINER", "N") == "Y"

  service_location <- remove_slash_if_added(Sys.getenv("GIT_CLONE_PATH"))
  if (service_location == "") {
    service_location <- dirname(dirname(dirname(normalizePath("."))))
  }

  if (!dir.exists(file.path(service_location, "armadillo"))) {
    stop("Service location is not in armadillo clone root.")
  }

  test_file_path <- remove_slash_if_added(Sys.getenv("TEST_FILE_PATH"))
  if (test_file_path == "") {
    testing_path <- file.path(service_location, "data", "testing")
    if (!dir.exists(testing_path)) {
      dir.create(testing_path, recursive = TRUE)
    }
    test_file_path <- testing_path
  }

  admin_pwd <- Sys.getenv("ADMIN_PASSWORD")
  user <- Sys.getenv("OIDC_EMAIL")

  if (user == "" && admin_pwd == "") {
    stop("User and admin password are both not set!")
  }

  ADMIN_MODE <- user == ""
  dest <- add_slash_if_not_added(test_file_path)
  app_info <- get_from_api("actuator/info", armadillo_url)
  version <- unlist(app_info$build$version)
  auth_type <- if (ADMIN_MODE) "basic" else "bearer"

  profile <- Sys.getenv("CONTAINER")
  if (profile == "") {
    stop("CONTAINER environment variable is not set. Please set it in .env file (e.g., CONTAINER=donkey)")
  }

  default_parquet_path <- add_slash_if_not_added(file.path(service_location, "data", "shared-lifecycle"))
  rda_dir <- file.path(test_file_path, "gse66351_1.rda")
  rda_url <- "https://github.com/isglobal-brge/brge_data_large/raw/master/data/gse66351_1.rda"
  update_auto <- ifelse(ADMIN_MODE, "n", "y")

  profile_defaults <- data.frame(
    name = c("xenon", "rock"),
    container = c("datashield/rock-dolomite-xenon:latest", "datashield/rock-base:latest"),
    port = c("", ""),
    whitelist = c("resourcer,dsMediation,dsMTLBase", ""),
    blacklist = c("", "")
  )

  options(timeout = 300)

  test_env$config <- list(
    skip_tests = skip_tests, armadillo_url = armadillo_url, interactive = interactive,
    user = user, admin_pwd = admin_pwd, test_file_path = test_file_path,
    service_location = service_location, dest = dest, app_info = app_info,
    version = version, auth_type = auth_type, as_docker_container = as_docker_container,
    ADMIN_MODE = ADMIN_MODE, profile = profile, default_parquet_path = default_parquet_path,
    rda_dir = rda_dir, update_auto = update_auto, profile_defaults = profile_defaults, rda_url = rda_url
  )

  # Set global variable for use in tests (named test_config to avoid httr::config collision)
  test_config <<- test_env$config

  cli_verbose_success("Configuration loaded")
  invisible(test_env$config)
}

# -----------------------------------------------------------------------------
# Level 2: Downloads
# -----------------------------------------------------------------------------

ensure_tables_downloaded <- function() {
  ensure_config()
  if (isTRUE(test_env$tables_downloaded)) {
    return(invisible(TRUE))
  }

  cli_verbose_info("Ensuring test tables are available...")
  config <- test_env$config

  dest <- if (dir.exists(config$default_parquet_path)) {
    config$default_parquet_path
  } else {
    config$dest
  }

  if (!dir.exists(config$default_parquet_path)) {
    cli_verbose_info("Downloading test tables...")
    create_dir_if_not_exists(config$dest, "core")
    create_dir_if_not_exists(config$dest, "outcome")
    create_dir_if_not_exists(config$dest, "survival")

    base_url <- "https://github.com/molgenis/molgenis-service-armadillo/raw/master/data/shared-lifecycle/%s/%s.parquet"
    files <- list(
      c("core", "nonrep"), c("core", "yearlyrep"), c("core", "monthlyrep"),
      c("core", "trimesterrep"), c("outcome", "nonrep"), c("outcome", "yearlyrep"),
      c("survival", "veteran")
    )

    for (f in files) {
      download.file(sprintf(base_url, f[1], f[2]), paste0(config$dest, f[1], "/", f[2], ".parquet"), quiet = TRUE)
    }
    cli_verbose_success("Tables downloaded")
  } else {
    cli_verbose_success("Tables available locally")
  }

  # Verify required tables exist and can be read
  required_tables <- c("core/nonrep.parquet", "core/yearlyrep.parquet", "survival/veteran.parquet")
  for (table in required_tables) {
    table_path <- paste0(dest, table)
    if (!file.exists(table_path)) {
      stop(sprintf("Required table not found: %s", table_path))
    }
    # Verify file can be read
    tryCatch(
      {
        df <- arrow::read_parquet(table_path)
        if (nrow(df) == 0) {
          stop(sprintf("Table is empty: %s", table_path))
        }
      },
      error = function(e) {
        stop(sprintf("Cannot read table %s: %s", table_path, e$message))
      }
    )
  }

  cli_verbose_success("Tables verified")
  test_env$tables_downloaded <- TRUE
  invisible(TRUE)
}

ensure_resources_downloaded <- function() {
  ensure_config()
  if (isTRUE(test_env$resources_downloaded)) {
    return(invisible(TRUE))
  }

  cli_verbose_info("Ensuring test resources are available...")
  config <- test_env$config

  if (!file.exists(config$rda_dir)) {
    cli_verbose_info(sprintf("Downloading resource from %s...", config$rda_url))
    download.file(config$rda_url, config$rda_dir, quiet = TRUE)
  }

  if (!file.exists(config$rda_dir)) {
    stop(sprintf("Resource file [%s] doesn't exist after download attempt", config$rda_dir))
  }

  cli_verbose_success("Resources available")
  test_env$resources_downloaded <- TRUE
  invisible(TRUE)
}

# -----------------------------------------------------------------------------
# Level 3: Authentication (TOKENS)
# -----------------------------------------------------------------------------

#' Ensure authentication tokens are obtained
#'
#' This function obtains BOTH tokens exactly once:
#' - Researcher token (armadillo.get_token) - used for API calls and DSI login
#' - Data manager login (armadillo.login) - used for armadillo.* functions
#'
#' After this function runs, test_env$token contains the researcher token
#' and the DM session is active for armadillo.* calls.
ensure_tokens <- function() {
  ensure_config()

  if (isTRUE(test_env$tokens_obtained)) {
    return(invisible(TRUE))
  }

  config <- test_env$config
  armadillo_url <<- config$armadillo_url # Set global for legacy functions

  # -------------------------------------------------------------------------
  # TOKEN 1: Researcher token (for API calls + DSI login)
  # -------------------------------------------------------------------------
  cli::cli_alert_info("Obtaining researcher token...")

  if (config$ADMIN_MODE) {
    test_env$token <- config$admin_pwd
    cli::cli_alert_success("Using admin password as token")
  } else {
    existing_token <- Sys.getenv("TOKEN")
    if (existing_token != "") {
      cli::cli_alert_success("Using TOKEN from environment")
      test_env$token <- existing_token
    } else {
      test_env$token <- MolgenisArmadillo::armadillo.get_token(config$armadillo_url)
      cli::cli_alert_success("Researcher token obtained")
    }
  }

  # -------------------------------------------------------------------------
  # TOKEN 2: Data manager login (for armadillo.* functions)
  # -------------------------------------------------------------------------
  cli::cli_alert_info("Logging in as data manager...")

  if (config$ADMIN_MODE) {
    MolgenisArmadillo::armadillo.login_basic(config$armadillo_url, "admin", config$admin_pwd)
  } else {
    MolgenisArmadillo::armadillo.login(config$armadillo_url)
  }

  cli::cli_alert_success("Data manager login complete")

  # -------------------------------------------------------------------------
  # Verify admin/DM rights by attempting to list projects
  # -------------------------------------------------------------------------
  cli_verbose_info("Verifying admin permissions...")

  tryCatch(
    {
      projects <- MolgenisArmadillo::armadillo.list_projects()
      cli_verbose_success("Admin permissions verified")
    },
    error = function(e) {
      cli::cli_alert_danger("PERMISSION ERROR: You do not have admin/data manager rights!")
      cli::cli_alert_warning("This can happen if:")
      cli::cli_ul(c(
        "A previous test run failed to restore your permissions in teardown",
        "Your OIDC token expired",
        "You were never granted admin rights on this Armadillo instance"
      ))
      cli::cli_alert_info(sprintf("Please ask an admin to restore your permissions for: %s", config$user))
      cli::cli_alert_info(sprintf("Armadillo URL: %s", config$armadillo_url))
      stop("Cannot proceed without admin permissions. See messages above.")
    }
  )

  test_env$tokens_obtained <- TRUE
  invisible(TRUE)
}

# -----------------------------------------------------------------------------
# Level 4: Admin setup (project, data, resources)
# -----------------------------------------------------------------------------

ensure_project_created <- function() {
  ensure_tokens()

  if (!is.null(test_env$project)) {
    return(invisible(test_env$project))
  }

  config <- test_env$config

  # Profile will be validated when researcher logs in via DSI
  cli_verbose_info(sprintf("Using profile [%s]", config$profile))

  # Generate random project name
  current_projects <- MolgenisArmadillo::armadillo.list_projects()
  random_project <- stringi::stri_rand_strings(1, 10, "[a-z0-9]")
  while (random_project %in% current_projects) {
    random_project <- stringi::stri_rand_strings(1, 10, "[a-z0-9]")
  }
  test_env$project <- random_project

  # Create project
  cli_verbose_info(sprintf("Creating project [%s]...", test_env$project))
  MolgenisArmadillo::armadillo.create_project(test_env$project)

  # Set global variable
  project <<- test_env$project

  cli_verbose_success(sprintf("Project [%s] created", test_env$project))
  invisible(test_env$project)
}

ensure_tables_uploaded <- function() {
  ensure_tables_downloaded()
  ensure_project_created()

  if (isTRUE(test_env$tables_uploaded)) {
    return(invisible(TRUE))
  }

  config <- test_env$config
  dest <- if (dir.exists(config$default_parquet_path)) config$default_parquet_path else config$dest

  cli_verbose_info("Uploading test tables...")

  # Core tables
  nonrep <- arrow::read_parquet(paste0(dest, "core/nonrep.parquet"))
  yearlyrep <- arrow::read_parquet(paste0(dest, "core/yearlyrep.parquet"))
  monthlyrep <- arrow::read_parquet(paste0(dest, "core/monthlyrep.parquet"))
  trimesterrep <- arrow::read_parquet(paste0(dest, "core/trimesterrep.parquet"))

  # Validate data integrity (column names check)
  expected_cols <- c("row_id", "child_id", "age_trimester", "smk_t", "alc_t")
  if (!identical(colnames(trimesterrep), expected_cols)) {
    cli::cli_alert_danger("Data integrity check failed!")
    cli::cli_alert_warning(sprintf("Expected columns: %s", paste(expected_cols, collapse = ", ")))
    cli::cli_alert_warning(sprintf("Actual columns: %s", paste(colnames(trimesterrep), collapse = ", ")))
    stop("Trimesterrep column names incorrect - test data may be corrupted")
  }

  MolgenisArmadillo::armadillo.upload_table(test_env$project, "2_1-core-1_0", nonrep)
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "2_1-core-1_0", yearlyrep)
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "2_1-core-1_0", monthlyrep)
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "2_1-core-1_0", trimesterrep)
  rm(nonrep, yearlyrep, monthlyrep, trimesterrep)

  # Outcome tables
  nonrep <- arrow::read_parquet(paste0(dest, "outcome/nonrep.parquet"))
  yearlyrep <- arrow::read_parquet(paste0(dest, "outcome/yearlyrep.parquet"))
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "1_1-outcome-1_0", nonrep)
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "1_1-outcome-1_0", yearlyrep)
  rm(nonrep, yearlyrep)

  # Survival table
  veteran <- arrow::read_parquet(paste0(dest, "survival/veteran.parquet"))
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "survival", veteran)
  rm(veteran)

  # Tidyverse table
  MolgenisArmadillo::armadillo.upload_table(test_env$project, "tidyverse", mtcars)

  cli_verbose_success("Tables uploaded")
  test_env$tables_uploaded <- TRUE
  invisible(TRUE)
}

ensure_resources_uploaded <- function() {
  ensure_resources_downloaded()
  ensure_project_created()

  if (isTRUE(test_env$resources_uploaded)) {
    return(invisible(TRUE))
  }

  config <- test_env$config

  cli_verbose_info("Uploading test resources...")

  # Upload resource file
  source(file.path(test_env$test_cases_dir, "upload-resource.R"))
  upload_resource(
    project = test_env$project, rda_dir = config$rda_dir, url = config$armadillo_url,
    token = test_env$token, folder = "ewas", file_name = "gse66351_1.rda",
    auth_type = config$auth_type, skip_tests = NULL
  )

  # Create resource definition
  source(file.path(test_env$test_cases_dir, "create-resource.R"))
  resGSE1 <- create_resource(
    target_project = test_env$project, url = config$armadillo_url, folder = "ewas",
    file_name = "gse66351_1.rda", resource_name = "GSE66351_1", format = "ExpressionSet", skip_tests = NULL
  )

  MolgenisArmadillo::armadillo.upload_resource(project = test_env$project, folder = "ewas", resource = resGSE1, name = "GSE66351_1")

  cli_verbose_success("Resources uploaded")
  test_env$resources_uploaded <- TRUE
  invisible(TRUE)
}

# Legacy function for backwards compatibility - uploads both tables and resources
ensure_admin_setup <- function() {
  ensure_tables_uploaded()
  ensure_resources_uploaded()
  test_env$admin_setup <- TRUE
  invisible(TRUE)
}

# -----------------------------------------------------------------------------
# Level 5: Researcher connection
# -----------------------------------------------------------------------------

# Internal helper to get profile info and set permissions
.setup_researcher_permissions <- function() {
  if (isTRUE(test_env$researcher_permissions_set)) {
    return(invisible(TRUE))
  }

  ensure_project_created()
  config <- test_env$config

  # Get profile info (needed for checking packageWhitelist)
  cli_verbose_info(sprintf("Fetching profile info for [%s]...", config$profile))
  profile_endpoint <- paste0(config$armadillo_url, "ds-profiles/", config$profile)
  auth_header <- get_auth_header(config$auth_type, test_env$token)
  response <- httr::GET(profile_endpoint, config = c(httr::add_headers(auth_header)))
  if (response$status_code == 200) {
    test_env$profile_info <- httr::content(response)
  } else {
    cli_verbose_info(sprintf("Could not fetch profile info (status %d)", response$status_code))
    test_env$profile_info <- list(packageWhitelist = character(0))
  }

  # Set researcher permissions if not in admin mode
  if (!config$ADMIN_MODE) {
    source(file.path(test_env$test_cases_dir, "set_researcher_access.R"))
    set_researcher_access(
      url = config$armadillo_url, interactive = config$interactive,
      required_projects = list(test_env$project), user = config$user,
      admin_pwd = config$admin_pwd, update_auto = config$update_auto, skip_tests = NULL
    )
  }

  test_env$researcher_permissions_set <- TRUE
  invisible(TRUE)
}

# Researcher login - just establishes connection, no table assignment
ensure_researcher_login <- function() {
  ensure_project_created()
  .setup_researcher_permissions()

  if (!is.null(test_env$conns)) {
    return(invisible(test_env$conns))
  }

  cli_verbose_info("Setting up researcher connection...")
  config <- test_env$config

  # Suppress "Secure HTTP connection is recommended" warning for localhost testing
  logindata <- suppressWarnings(create_dsi_builder(
    url = config$armadillo_url,
    profile = config$profile,
    password = config$admin_pwd,
    token = test_env$token,
    ADMIN_MODE = config$ADMIN_MODE
  ))

  cli_verbose_info(sprintf("Logging in as researcher with profile [%s]...", config$profile))
  test_env$conns <- DSI::datashield.login(logins = logindata, assign = FALSE)

  # Set global variables for use in tests
  conns <<- test_env$conns
  project <<- test_env$project

  cli_verbose_success("Researcher connection established")
  invisible(test_env$conns)
}

# Researcher login AND assign tables - for table-based tests
ensure_researcher_login_and_assign <- function() {
  ensure_tables_uploaded()
  .setup_researcher_permissions()

  if (!is.null(test_env$conns)) {
    return(invisible(test_env$conns))
  }

  cli_verbose_info("Setting up researcher connection with table assignment...")
  config <- test_env$config

  # Suppress "Secure HTTP connection is recommended" warning for localhost testing
  logindata <- suppressWarnings(create_dsi_builder(
    url = config$armadillo_url,
    profile = config$profile,
    password = config$admin_pwd,
    token = test_env$token,
    table = sprintf("%s/2_1-core-1_0/nonrep", test_env$project),
    ADMIN_MODE = config$ADMIN_MODE
  ))

  cli_verbose_info(sprintf("Logging in as researcher with profile [%s]...", config$profile))
  test_env$conns <- DSI::datashield.login(logins = logindata, symbol = "nonrep", assign = TRUE)

  # Set global variables for use in tests
  conns <<- test_env$conns
  project <<- test_env$project

  cli_verbose_success("Researcher connection established with tables assigned")
  invisible(test_env$conns)
}

# -----------------------------------------------------------------------------
# Profile reset (for multi-profile runs)
# -----------------------------------------------------------------------------

#' Reset per-profile state for a new profile iteration
#'
#' Clears all state that is specific to a profile run (project, connections,
#' upload flags, etc.) while preserving config, downloads, and authentication
#' tokens. Called between profile iterations in multi-profile runs.
#'
#' @param new_profile The new profile name to switch to
reset_for_new_profile <- function(new_profile) {
  # Update profile in config
  test_env$config$profile <- new_profile
  test_config$profile <<- new_profile

  # Clear per-profile state
  # Preserved: config (updated above), tables_downloaded, resources_downloaded,
  #            tokens_obtained, token, verbose_mode, testthat_dir, test_cases_dir
  test_env$conns <- NULL
  test_env$project <- NULL
  test_env$tables_uploaded <- NULL
  test_env$resources_uploaded <- NULL
  test_env$admin_setup <- NULL
  test_env$researcher_permissions_set <- NULL
  test_env$profile_info <- NULL
  test_env$omics_setup <- NULL
  test_env$exposome_setup <- NULL

  # Clear globals
  if (exists("conns", envir = .GlobalEnv)) rm("conns", envir = .GlobalEnv)
  if (exists("project", envir = .GlobalEnv)) rm("project", envir = .GlobalEnv)
}

# -----------------------------------------------------------------------------
# Helper functions
# -----------------------------------------------------------------------------

should_skip_test <- function(test_name) {
  ensure_config()
  return(any(test_env$config$skip_tests %in% test_name))
}

skip_if_excluded <- function(test_name) {
  if (should_skip_test(test_name)) {
    testthat::skip(paste("Excluded via SKIP_TESTS:", test_name))
  }
}

add_slash_if_not_added <- function(path) {
  if (!endsWith(path, "/")) paste0(path, "/") else path
}

remove_slash_if_added <- function(path) {
  if (endsWith(path, "/")) gsub("/$", "", path) else path
}

create_dir_if_not_exists <- function(dest, directory) {
  full_path <- paste0(dest, directory)
  if (!dir.exists(full_path)) dir.create(full_path, recursive = TRUE)
}

get_from_api <- function(endpoint, armadillo_url) {
  httr::content(httr::GET(paste0(armadillo_url, endpoint)))
}
