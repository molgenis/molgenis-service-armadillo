# Setup
test_name <- "researcher_login"
table <- "2_1-core-1_0/nonrep"
object <- "nonrep"
variables <- "coh_country"

test_that("researcher login", {
  do_skip_test(test_name)
  full_table <- sprintf("%s/%s", release_env$project1, table)
  # Suppress "Secure HTTP connection is recommended" warning for localhost
  logindata <- suppressWarnings(create_dsi_builder(table = full_table))
  release_env$conns <- datashield.login(logins = logindata, symbol = object, variables = variables, assign = TRUE)
  expect_true(!is.null(release_env$conns))
})
