# Setup
test_name <- "permissions"

# Helper: call an admin-intended endpoint with the researcher's token and expect 403
expect_forbidden <- function(method, endpoint, body = NULL) {
  url <- paste0(release_env$armadillo_url, endpoint)
  auth_header <- get_auth_header("bearer", release_env$token)
  config <- c(httr::content_type_json(), httr::add_headers(auth_header))

  response <- switch(toupper(method),
    "GET" = httr::GET(url, config = config),
    "HEAD" = httr::HEAD(url, config = config),
    "POST" = httr::POST(url, body = body, encode = "json", config = config),
    "PUT" = httr::PUT(url, body = body, encode = "json", config = config),
    "DELETE" = httr::DELETE(url, body = body, encode = "json", config = config),
    stop(sprintf("Unsupported method: %s", method))
  )

  status <- httr::status_code(response)
  testthat::expect(
    status == 403,
    sprintf(
      "%s %s is not admin-protected: researcher got HTTP %d (expected 403 Forbidden)",
      method, endpoint, status
    )
  )
}

test_that("access endpoints are admin-only", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  # Read endpoints
  expect_forbidden("GET", "access")
  expect_forbidden("GET", "access/permissions")
  expect_forbidden("GET", "access/projects")
  expect_forbidden("GET", "access/projects/nonexistent-project")
  expect_forbidden("GET", "access/users")
  expect_forbidden("GET", "access/users/nonexistent@example.com")

  # Write endpoints
  permission_body <- jsonlite::toJSON(
    list(email = release_env$user, project = release_env$project1),
    auto_unbox = TRUE
  )
  expect_forbidden("POST", "access/permissions", permission_body)
  expect_forbidden("DELETE", "access/permissions", permission_body)

  project_body <- jsonlite::toJSON(
    list(name = release_env$project1, users = list(release_env$user)),
    auto_unbox = TRUE
  )
  expect_forbidden("PUT", "access/projects", project_body)
  expect_forbidden("DELETE", "access/projects/nonexistent-project")

  user_body <- jsonlite::toJSON(
    list(email = release_env$user, admin = FALSE, projects = list(release_env$project1)),
    auto_unbox = TRUE
  )
  expect_forbidden("PUT", "access/users", user_body)
  expect_forbidden("DELETE", "access/users/nonexistent@example.com")
})

test_that("profiles endpoints are admin-only", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  profile_body <- jsonlite::toJSON(
    list(name = "nonexistent-test-profile", image = "dummy", host = "localhost", port = 6311),
    auto_unbox = TRUE
  )
  expect_forbidden("PUT", "ds-profiles", profile_body)
  expect_forbidden("DELETE", "ds-profiles/nonexistent-test-profile")
})

test_that("insight endpoints are admin-only", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  expect_forbidden("GET", "insight/files")
})

test_that("development endpoints are admin-only", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  expect_forbidden("GET", "whitelist")
})

test_that("storage download is admin-only", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  object <- utils::URLencode("2_1-core-1_0/nonrep", reserved = TRUE)
  expect_forbidden("GET", sprintf("storage/projects/%s/objects/%s", release_env$project1, object))
})

# ---- Cross-project data access ----

test_that("setup: create project2 with table for cross-project tests", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  release_env$security_project2 <- generate_random_project_name()

  # Elevate to admin, create project, upload table
  set_user(TRUE, list(release_env$project1))
  armadillo.create_project(release_env$security_project2)
  armadillo.upload_table(release_env$security_project2, "test", mtcars)
  set_user(FALSE, list(release_env$project1))

  succeed()
})

test_that("researcher cannot check table existence in project without permission", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")
  skip_if(is.null(release_env$security_project2), "Setup failed")

  table_path <- sprintf("tables/%s/test/mtcars", release_env$security_project2)
  expect_forbidden("HEAD", table_path)
})

test_that("researcher cannot check resource existence in project without permission", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")
  skip_if(is.null(release_env$security_project2), "Setup failed")

  resource_path <- sprintf("resources/%s/test/nonexistent", release_env$security_project2)
  expect_forbidden("HEAD", resource_path)
})

test_that("researcher cannot list objects in project without permission", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")
  skip_if(is.null(release_env$security_project2), "Setup failed")

  objects_path <- sprintf("storage/projects/%s/objects", release_env$security_project2)
  expect_forbidden("GET", objects_path)
})

test_that("researcher cannot check object existence in project without permission", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")
  skip_if(is.null(release_env$security_project2), "Setup failed")

  object <- utils::URLencode("test/mtcars.parquet", reserved = TRUE)
  object_path <- sprintf("storage/projects/%s/objects/%s", release_env$security_project2, object)
  expect_forbidden("HEAD", object_path)
})

test_that("researcher cannot see tables from project without permission (DSI)", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")
  skip_if(is.null(release_env$security_project2), "Setup failed")

  dsi_tables <- datashield.tables(release_env$conns)
  project2_tables <- grep(release_env$security_project2, dsi_tables$armadillo, value = TRUE)
  expect_length(project2_tables, 0)
})

test_that("researcher cannot load table from project without permission (HTTP)", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")
  skip_if(is.null(release_env$security_project2), "Setup failed")

  full_table <- sprintf("%s/test/mtcars", release_env$security_project2)
  url <- paste0(release_env$armadillo_url, "load-table")
  auth_header <- get_auth_header("bearer", release_env$token)
  response <- httr::POST(url,
    query = list(symbol = "tbl_denied", table = full_table),
    httr::add_headers(auth_header)
  )
  expect_equal(httr::status_code(response), 403)
})

test_that("researcher cannot assign table from project without permission (DSI)", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")
  skip_if(is.null(release_env$security_project2), "Setup failed")

  # NOTE: DSMolgenisArmadillo .handle_request_error does not handle 403 responses,
  # so datashield.assign.table silently fails instead of throwing an error.
  # This test verifies the table was NOT actually assigned.
  full_table <- sprintf("%s/test/mtcars", release_env$security_project2)
  datashield.assign.table(release_env$conns, "tbl_denied", full_table)

  symbols <- ds.ls(datasources = release_env$conns)
  has_denied <- "tbl_denied" %in% symbols$armadillo$objects.found
  expect_false(has_denied,
    info = "Table from project without access should not be assigned")
})

test_that("cleanup: delete project2 for cross-project tests", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")
  skip_if(is.null(release_env$security_project2), "Setup failed")

  set_user(TRUE, list(release_env$project1))
  armadillo.delete_project(release_env$security_project2)
  set_user(FALSE, list(release_env$project1))

  succeed()
})
