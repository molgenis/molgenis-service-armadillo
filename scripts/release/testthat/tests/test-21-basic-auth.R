# Setup
test_name <- "basic_auth"

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

test_that("failed logins trigger lockout after threshold", {
  do_skip_test(test_name)
  skip_if(release_env$admin_pwd == "", "admin password not available")

  url <- paste0(release_env$armadillo_url, "basic-login")
  wrong_header <- create_basic_header("wrongpassword")

  # Send FREE_ATTEMPTS (5) wrong passwords — should all return plain 401
  for (i in 1:5) {
    resp <- httr::GET(url, httr::add_headers("Authorization" = wrong_header))
    expect_equal(httr::status_code(resp), 401)
  }

  # 6th attempt triggers lockout — should return 401 with lockout JSON
  resp <- httr::GET(url, httr::add_headers("Authorization" = wrong_header))
  expect_equal(httr::status_code(resp), 401)
  body <- httr::content(resp, as = "parsed")
  expect_true(body$locked)
  expect_true(body$secondsRemaining > 0)
  expect_false(is.null(resp$headers[["retry-after"]]))

  # 7th attempt — still locked, longer duration
  resp <- httr::GET(url, httr::add_headers("Authorization" = wrong_header))
  expect_equal(httr::status_code(resp), 401)
  body2 <- httr::content(resp, as = "parsed")
  expect_true(body2$locked)
  expect_true(body2$secondsRemaining >= body$secondsRemaining)
})
