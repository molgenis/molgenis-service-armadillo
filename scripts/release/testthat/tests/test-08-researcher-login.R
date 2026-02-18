# Setup
test_name <- "researcher_login"
table <- "2_1-core-1_0/nonrep"
object <- "nonrep"
variables <- "coh_country"

create_dsi_builder <- function(server = "armadillo", table = "", resource = "") {
  builder <- DSI::newDSLoginBuilder()
  if (release_env$ADMIN_MODE) {
    builder$append(
      server = server,
      url = release_env$armadillo_url,
      profile = release_env$current_profile,
      table = table,
      driver = "ArmadilloDriver",
      user = "admin",
      password = release_env$admin_pwd,
      resource = resource
    )
  } else {
    builder$append(
      server = server,
      url = release_env$armadillo_url,
      profile = release_env$current_profile,
      table = table,
      driver = "ArmadilloDriver",
      token = release_env$token,
      resource = resource
    )
  }
  return(builder$build())
}

test_that("no leaked objects after login", {
  do_skip_test(test_name)
  logindata <- suppressWarnings(create_dsi_builder())
  conns <- datashield.login(logins = logindata, assign = FALSE)

  result <- ds.ls(datasources = conns)
  expect_equal(result[[1]]$objects.found, character(0))

  datashield.logout(conns)
})

test_that("researcher login", {
  do_skip_test(test_name)
  full_table <- sprintf("%s/%s", release_env$project1, table)
  # Suppress "Secure HTTP connection is recommended" warning for localhost
  logindata <- suppressWarnings(create_dsi_builder(table = full_table))
  release_env$conns <- datashield.login(logins = logindata, symbol = object, variables = variables, assign = TRUE)
  expect_true(!is.null(release_env$conns))
})
