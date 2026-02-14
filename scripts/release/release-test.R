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

profiles <- unlist(stri_split_fixed(release_env$profile, ","))


run_tests_for_profile <- function(profile) {
    # Logout previous DataSHIELD session to avoid stale connections across profile runs
    if (!is.null(release_env$conns)) {
      tryCatch(datashield.logout(release_env$conns), error = function(e) {})
      release_env$conns <- NULL
    }

    release_env$current_profile <- profile
    release_env$project1 <- generate_random_project_name()

    cat("\n\n")
    cli_h1(paste0("Testing profile: ", profile))
    cli_alert_info(sprintf("Project name: %s", release_env$project1))
    setup_profiles()

    testthat::test_dir(
      "testthat/tests",
      reporter = testthat::ProgressReporter$new(show_praise = FALSE, min_time = 0),
      stop_on_failure = FALSE
    )
}

invisible(lapply(profiles, run_tests_for_profile))

cat("\n")
cli_alert_info("Please test rest of UI manually, if impacted by this release")
