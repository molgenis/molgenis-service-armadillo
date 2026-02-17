# Setup
test_name <- "basic-auth"

test_that("basic authentication works end-to-end", {
  do_skip_test(test_name)
  skip_if(release_env$admin_pwd == "", "admin password not available")

  dest <- get_tables_path()

  armadillo.login_basic(release_env$armadillo_url, "admin", release_env$admin_pwd)
  project <- generate_random_project_name()
  armadillo.create_project(project)
  nonrep <- arrow::read_parquet(paste0(dest, "core/nonrep.parquet"))
  armadillo.upload_table(project, "2_1-core-1_0", nonrep)
  rm(nonrep)
  expect_true(project %in% armadillo.list_projects())
  table <- sprintf("%s/2_1-core-1_0/nonrep", project)
  expect_true(table %in% armadillo.list_tables(project))
  armadillo.delete_project(project)
})
