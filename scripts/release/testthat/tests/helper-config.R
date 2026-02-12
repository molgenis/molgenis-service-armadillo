library(stringr)
library(tibble)

# Display two library groups side by side
show_version_info_combined <- function(libs1, label1, libs2, label2) {
  versions1 <- sapply(libs1, function(lib) as.character(packageVersion(lib)))
  versions2 <- sapply(libs2, function(lib) as.character(packageVersion(lib)))

  max_name1 <- max(nchar(libs1))
  max_name2 <- max(nchar(libs2))
  max_ver1 <- max(nchar(versions1))

  # Calculate column width for left side
  col_width <- max_name1 + max_ver1 + 4

  # Print headers
  cat(sprintf("%-*s%s\n", col_width + 4, label1, label2))

  # Print rows
  max_rows <- max(length(libs1), length(libs2))
  for (i in 1:max_rows) {
    if (i <= length(libs1)) {
      left <- sprintf("  %-*s %-*s", max_name1, libs1[i], max_ver1, versions1[i])
    } else {
      left <- sprintf("  %-*s", col_width - 2, "")
    }
    if (i <= length(libs2)) {
      right <- sprintf("  %-*s %s", max_name2, libs2[i], versions2[i])
    } else {
      right <- ""
    }
    cat(sprintf("%-*s%s\n", col_width, left, right))
  }
}

configure_test <- function() {
  cli_progress_step("Reading config from '.env'")
  suppressWarnings(readRenviron(".env"))

  skip_tests <- Sys.getenv("SKIP_TESTS")
  skip_tests <- str_split(skip_tests, ",")[[1]]

  armadillo_url <- Sys.getenv("ARMADILLO_URL")
  if (armadillo_url == "") {
    cli_progress_done(result = "failed")
    cli_alert_warning("ARMADILLO_URL not set, defaulting to https://armadillo-demo.molgenis.net/")
    armadillo_url <- "https://armadillo-demo.molgenis.net/"
  } else {
    cli_progress_done()
  }

  if(str_detect(armadillo_url, "localhost") & !any(skip_tests %in% "ds-omics")){
    skip_tests <- c(skip_tests, "ds-omics")
  }


  interactive <- TRUE
  if (Sys.getenv("INTERACTIVE") == "N") {
    interactive <- FALSE
  }

  armadillo_url <- add_slash_if_not_added(armadillo_url)

  cli_progress_step(sprintf("Checking %s exists", armadillo_url))
  if (url.exists(armadillo_url)) {
    cli_progress_done()
    if (!startsWith(armadillo_url, "http")) {
      if (startsWith(armadillo_url, "localhost")) {
        armadillo_url <- paste0("http://", armadillo_url)
      } else {
        armadillo_url <- paste0("https://", armadillo_url)
      }
    }
  } else {
    cli_progress_done(result = "failed")
    exit_test(sprintf("URL [%s] doesn't exist", armadillo_url))
  }

  as_docker_container <- FALSE
  if ("Y" == Sys.getenv("AS_DOCKER_CONTAINER", "N")) {
    as_docker_container <- TRUE
  }

  service_location <- remove_slash_if_added(Sys.getenv("GIT_CLONE_PATH"))
  if (service_location == "") {
    service_location <- dirname(dirname(normalizePath(".")))
  }

  if (!dir.exists(file.path(service_location, "armadillo"))) {
    exit_test("Service location is not in armadillo clone root.")
  }

  test_file_path <- remove_slash_if_added(Sys.getenv("TEST_FILE_PATH"))
  if (test_file_path == "") {
    testing_path <- file.path(service_location, "data", "testing")
    if (!dir.exists(testing_path)) {
      cli_alert_info(sprintf("Creating test data directory: %s", testing_path))
      dir.create(testing_path)
    }
    test_file_path <- testing_path
  }

  admin_pwd <- Sys.getenv("ADMIN_PASSWORD")
  if (admin_pwd == "") {
    cli_alert_danger("Admin password not set in .env file, disabling admin mode.")
  }

  user <- Sys.getenv("OIDC_EMAIL")

  if (user == "" && admin_pwd == "") {
    exit_test("User and admin password are both not set!")
  }

  if (user == "") {
    cli_alert_danger("User not set in .env config!")
    cli_alert_info("Enabling admin mode")
    ADMIN_MODE <- TRUE
  } else {
    ADMIN_MODE <- FALSE
  }

  dest <- add_slash_if_not_added(test_file_path)

  version <- trimws(system("git describe --tags --abbrev=0 2>/dev/null", intern = TRUE))

  auth_type <- get_auth_type(ADMIN_MODE)

  # Override to TRUE - assume profiles are managed externally (already running)
  as_docker_container <- TRUE

  profile <- Sys.getenv("PROFILE")
  if (profile == "") {
    cli_alert_warning("Profile not set, defaulting to xenon.")
    profile <- "xenon"
  }

  default_parquet_path <- file.path(service_location, "data", "shared-lifecycle")
  default_parquet_path <- add_slash_if_not_added(default_parquet_path)

  rda_dir <- file.path(test_file_path, "gse66351_1.rda")
  rda_url <- "https://github.com/isglobal-brge/brge_data_large/raw/master/data/gse66351_1.rda"
  update_auto <- ifelse(ADMIN_MODE, "n", "y")

  # default profile settings in case a profile is missing
  profile_defaults <- data.frame(
    name = c("xenon", "rock"),
    container = c("datashield/rock-dolomite-xenon:latest", "datashield/rock-base:latest"),
    port = c("", ""),
    # Multiple packages can be concatenated using ,, then using stri_split_fixed() to break them up again
    # Not adding dsBase since that is always(?) required
    whitelist = c("resourcer,dsMediation,dsMTLBase", ""),
    blacklist = c("", "")
  )

  options(timeout = 300)

  release_env$skip_tests <- skip_tests
  release_env$armadillo_url <- armadillo_url
  release_env$interactive <- interactive
  release_env$user <- user
  release_env$admin_pwd <- admin_pwd
  release_env$test_file_path <- test_file_path
  release_env$service_location <- service_location
  release_env$dest <- dest
  release_env$version <- version
  release_env$auth_type <- auth_type
  release_env$as_docker_container <- as_docker_container
  release_env$ADMIN_MODE <- ADMIN_MODE
  release_env$profile <- profile
  release_env$default_parquet_path <- default_parquet_path
  release_env$rda_dir <- rda_dir
  release_env$update_auto <- update_auto
  release_env$profile_defaults <- profile_defaults
  release_env$rda_url <- rda_url

  # Generate random project name (actual creation happens in test-03)
  cli_progress_step("Generating random project name")
  release_env$project1 <- generate_random_project_name()
  cli_progress_done()
}
