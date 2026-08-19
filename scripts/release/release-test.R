#!/usr/bin/env -S Rscript --no-init-file
cat("
  __  __  ____  _      _____ ______ _   _ _____  _____                                     _ _ _ _
 |  \\/  |/ __ \\| |    / ____|  ____| \\ | |_   _|/ ____|     /\\                            | (_) | |
 | \\  / | |  | | |   | |  __| |__  |  \\| | | | | (___      /  \\   _ __ _ __ ___   __ _  __| |_| | | ___
 | |\\/| | |  | | |   | | |_ |  __| | . ` | | |  \\___ \\    / /\\ \\ | '__| '_ ` _ \\ / _` |/ _` | | | |/ _ \\
 | |  | | |__| | |___| |__| | |____| |\\  |_| |_ ____) |  / ____ \\| |  | | | | | | (_| | (_| | | | | (_) |
 |_|  |_|\\____/|______\\_____|______|_| \\_|_____|_____/  /_/    \\_\\_|  |_| |_| |_|\\__,_|\\__,_|_|_|_|\\___/

  _____      _                       _            _
 |  __ \\    | |                     | |          | |
 | |__) |___| | ___  __ _ ___  ___  | |_ ___  ___| |_
 |  _  // _ \\ |/ _ \\/ _` / __|/ _ \\ | __/ _ \\/ __| __|
 | | \\ \\  __/ |  __/ (_| \\__ \\  __/ | ||  __/\\__ \\ |_
 |_|  \\_\\___|_|\\___|\\__,_|___/\\___|  \\__\\___||___/\\__|
")

# Run all setup and initialization
source("lib/setup.R")

# Show test info once
cli_h1("Starting release test")
show_test_info()

containers <- unlist(stri_split_fixed(release_env$container, ","))
release_env$created_projects <- c()
release_env$admin_demoted <- FALSE

run_tests_for_container <- function(container) {
    release_env$current_container <- container

    cli_h2(paste0("Testing container: ", container))
    setup_containers()

    testthat::set_max_fails(Inf)
    testthat::test_dir(
      "testthat/tests",
      reporter = testthat::ProgressReporter$new(show_praise = FALSE, min_time = 0),
      stop_on_failure = FALSE
    )
}

tryCatch(
  invisible(lapply(containers, run_tests_for_container)),
  interrupt = function(i) {
    cat("\n")
    cli_alert_warning("Tests interrupted by user")
  },
  finally = {
    cat("\n")
    cli_h1("Teardown")
    teardown()
  }
)

cat("\n")
cli_alert_info("Please test rest of UI manually, if impacted by this release")
