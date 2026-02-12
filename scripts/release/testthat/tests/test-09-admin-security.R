# Setup
test_name <- "admin-security"

# Helper: call an admin-intended endpoint with the researcher's token and expect 403
expect_forbidden <- function(method, endpoint, body = NULL) {
  url <- paste0(release_env$armadillo_url, endpoint)
  auth_header <- get_auth_header("bearer", release_env$token)
  config <- c(httr::content_type_json(), httr::add_headers(auth_header))

  response <- switch(toupper(method),
    "GET" = httr::GET(url, config = config),
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
