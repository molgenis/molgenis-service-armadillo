show_test_info <- function() {
  test_name <- "show_test_info"
  if (should_skip_test(test_name)) {
    return()
  }

  admin_pwd_msg <- if (release_env$admin_pwd != "") "Yes" else "No"
  docker_mode <- if (release_env$as_docker_container) "Yes" else "No"
  admin_mode <- if (release_env$ADMIN_MODE) "Yes" else "No"
  skip_tests_filtered <- release_env$skip_tests[release_env$skip_tests != ""]
  skip_tests <- if (length(skip_tests_filtered) == 0) "None" else paste(skip_tests_filtered, collapse = ", ")

  cli_h2("Test information")
  cat(sprintf("
                      ,.-----__                       Testing version:      %s
                ,:::://///,:::-.                      Test server:          %s
               /:''/////// ``:::`;/|/                 OIDC User:            %s
              /'   ||||||     :://'`\\                 Admin password set:   %s
            .' ,   ||||||     `/(  e \\                Test data directory:  %s
      -===~__-'\\__X_`````\\_____/~`-._ `.              Admin-only mode:      %s
                  ~~        ~~       `~-'             Running in Docker:    %s
                                                      Project name:         %s
                                                      Skipping tests:       %s
    ", release_env$version, release_env$armadillo_url, release_env$user, admin_pwd_msg,
    release_env$dest, admin_mode, docker_mode, release_env$project1, skip_tests))

  cat("\n")
  cli_h2("Loaded packages")
  show_version_info_combined(
    c("MolgenisArmadillo", "DSI", "dsBaseClient", "DSMolgenisArmadillo", "resourcer",
      "dsSurvivalClient", "dsMediationClient", "dsMTLClient", "dsTidyverseClient",
      "dsExposomeClient", "dsOmicsClient"),
    "Armadillo/DataSHIELD:",
    c("getPass", "arrow", "httr", "jsonlite", "future", "purrr", "stringr", "tibble"),
    "Other:"
  )
  cat("\n")
}
