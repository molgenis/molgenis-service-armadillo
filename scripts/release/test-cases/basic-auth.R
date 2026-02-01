verify_basic_auth <- function() {
  test_name <- "basic_auth"

  test_that("basic authentication works end-to-end", {
    do_skip_test(test_name)
    skip_if(release_env$admin_pwd == "", "admin password not available")

    cli_h2("Basic authentication")
    cli_alert_info("Logging in as admin user")
    armadillo.login_basic(release_env$armadillo_url, "admin", release_env$admin_pwd)
    project <- generate_random_project_name()
    cli_alert_info(sprintf("Creating project [%s]", project))
    armadillo.create_project(project)
    nonrep <- arrow::read_parquet(paste0(release_env$default_parquet_path, "core/nonrep.parquet"))
    cli_alert_info(sprintf("Uploading file to [%s]", project))
    armadillo.upload_table(project, "2_1-core-1_0", nonrep)
    rm(nonrep)
    expect_true(project %in% armadillo.list_projects())
    table <- sprintf("%s/2_1-core-1_0/nonrep", project)
    expect_true(table %in% armadillo.list_tables(project))
    cli_alert_info(sprintf("Deleting [%s]", project))
    armadillo.delete_project(project)
  })
}
