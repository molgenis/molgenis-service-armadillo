#!/usr/bin/env Rscript
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
print(sessionInfo())

# Run all setup and initialization
source("lib/setup.R")

profiles <- unlist(stri_split_fixed(release_env$profile, ","))


run_tests_for_profile <- function(profile) {
    start_time <- Sys.time()
    release_env$current_profile <- profile
    cli_h2(paste0("Running for profile: ", profile))

    cli_h2("Determining whether to run with password or token")
    source("test-cases/set-admin-mode.R")
    set_admin_or_get_token()

    cli_h2("Configuring profiles")
    source("test-cases/setup-profiles.R")
    setup_profiles()

    cli_h1("Starting release test")
    source("lib/release-test-info.R")
    show_test_info()

    testthat::test_dir(
      "testthat/tests",
      reporter = testthat::ProgressReporter$new(),
      stop_on_failure = FALSE
    )

    cli_alert_info("Please test rest of UI manually, if impacted this release")
    end_time <- Sys.time()
    print(paste0("Running tests for profile [", profile, "] took: ", end_time - start_time))
}

lapply(profiles, run_tests_for_profile)
