create_dir_if_not_exists <- function(dest, directory) {
  if (!dir.exists(paste0(dest, directory))) {
    dir.create(paste0(dest, directory), recursive = TRUE)
  }
}

download_tables <- function() {
  test_name <- "download-tables"
  if (should_skip_test(test_name)) {
    return()
  }

  # Define all required table files
  table_files <- c(
    "core/nonrep.parquet",
    "core/yearlyrep.parquet",
    "core/monthlyrep.parquet",
    "core/trimesterrep.parquet",
    "outcome/nonrep.parquet",
    "outcome/yearlyrep.parquet",
    "survival/veteran.parquet"
  )

  # Check which files exist (try default path first, then dest)
  default_paths <- file.path(release_env$default_parquet_path, table_files)
  dest_paths <- file.path(release_env$dest, table_files)

  # Use default path if all files exist there
  if (all(file.exists(default_paths))) {
    cli_alert_success("Test tables available locally")
    return()
  }

  # Check which files are missing from dest
  missing_idx <- which(!file.exists(dest_paths))

  if (length(missing_idx) == 0) {
    cli_alert_success("Test tables available locally")
    return()
  }

  test_files_url_template <- "https://github.com/molgenis/molgenis-service-armadillo/raw/master/data/shared-lifecycle/%s"

  cli_alert_warning(paste0("Missing ", length(missing_idx), " table(s)"))
  cli_alert_info(paste0("Downloading from: ", sprintf(test_files_url_template, "")))
  cli_alert_info(paste0("Downloading into: ", release_env$dest))
  create_dir_if_not_exists(release_env$dest, "core")
  create_dir_if_not_exists(release_env$dest, "outcome")
  create_dir_if_not_exists(release_env$dest, "survival")

  for (i in seq_along(missing_idx)) {
    idx <- missing_idx[i]
    file <- table_files[idx]
    cli_alert_info(sprintf("Downloading %s (%d/%d)", file, i, length(missing_idx)))
    download_with_progress(sprintf(test_files_url_template, file), dest_paths[idx])
  }
  cli_alert_success("Test tables downloaded")
}
