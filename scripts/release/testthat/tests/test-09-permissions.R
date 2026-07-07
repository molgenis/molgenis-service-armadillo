# Setup
test_name <- "permissions"

# Build full API URL from an endpoint path
build_url <- function(endpoint) {
  paste0(release_env$armadillo_url, endpoint)
}

# Build httr config with JSON content type and bearer auth
build_config <- function() {
  auth_header <- get_auth_header("bearer", release_env$token)
  c(httr::content_type_json(), httr::add_headers(auth_header))
}

# Dispatch an HTTP method
dispatch_method <- function(method, url, body, config) {
  switch(toupper(method),
    "GET" = httr::GET(url, config = config),
    "HEAD" = httr::HEAD(url, config = config),
    "POST" = httr::POST(url, body = body, encode = "json", config = config),
    "PUT" = httr::PUT(url, body = body, encode = "json", config = config),
    "DELETE" = httr::DELETE(url, body = body, encode = "json", config = config),
    stop(sprintf("Unsupported method: %s", method))
  )
}

# Make an authenticated request to the Armadillo API
make_request <- function(method, endpoint, body = NULL) {
  dispatch_method(method, build_url(endpoint), body, build_config())
}

# Assert that an endpoint returns 403 for the current user
expect_forbidden <- function(method, endpoint, body = NULL) {
  response <- make_request(method, endpoint, body)
  status <- httr::status_code(response)
  testthat::expect(
    status == 403,
    sprintf(
      "%s %s is not admin-protected: researcher got HTTP %d (expected 403 Forbidden)",
      method, endpoint, status
    )
  )
}

# ---- Admin-only endpoints ----
# These endpoints require ROLE_SU. Researchers should always get 403.

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
  expect_forbidden("POST", "ds-profiles/default/start")
  expect_forbidden("POST", "ds-profiles/default/stop")
})

test_that("insight endpoints are admin-only", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  expect_forbidden("GET", "insight/files")
  expect_forbidden("GET", "insight/files/nonexistent")
  expect_forbidden("GET", "insight/files/nonexistent/download")
  expect_forbidden("GET", "insight/docker/all-images")
})

test_that("development endpoints are admin-only", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  expect_forbidden("GET", "whitelist")
  expect_forbidden("POST", "whitelist/dsBase")
  expect_forbidden("DELETE", "delete-docker-image?imageId=sha256:fake")

  # install-package (multipart upload)
  url <- build_url("install-package")
  auth_header <- get_auth_header("bearer", release_env$token)
  tmp <- tempfile(fileext = ".tar.gz")
  writeLines("dummy", tmp)
  response <- httr::POST(url,
    body = list(file = httr::upload_file(tmp)),
    httr::add_headers(auth_header)
  )
  unlink(tmp)
  expect_equal(httr::status_code(response), 403)
})

test_that("storage object operations are admin-only", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  object <- utils::URLencode("2_1-core-1_0/nonrep.parquet", reserved = TRUE)
  project_path <- sprintf("storage/projects/%s", release_env$project1)

  # Download
  expect_forbidden("GET", sprintf("%s/objects/%s", project_path, object))

  # Delete
  expect_forbidden("DELETE", sprintf("%s/objects/%s", project_path, object))

  # Move
  move_body <- jsonlite::toJSON(list(name = "test/moved.parquet"), auto_unbox = TRUE)
  expect_forbidden("POST", sprintf("%s/objects/%s/move", project_path, object), move_body)

  # Copy
  copy_body <- jsonlite::toJSON(list(name = "test/copied.parquet"), auto_unbox = TRUE)
  expect_forbidden("POST", sprintf("%s/objects/%s/copy", project_path, object), copy_body)

  # Preview
  expect_forbidden("GET", sprintf("%s/objects/%s/preview", project_path, object))

  # Metadata
  expect_forbidden("GET", sprintf("%s/objects/%s/metadata", project_path, object))

  # Variables
  expect_forbidden("GET", sprintf("%s/objects/%s/variables", project_path, object))

  # Info
  expect_forbidden("GET", sprintf("%s/objects/%s/info", project_path, object))

  # Link creation
  link_body <- jsonlite::toJSON(list(
    sourceProject = release_env$project1,
    sourceObjectName = "2_1-core-1_0/nonrep",
    linkedObject = "test-link",
    variables = "child_id"
  ), auto_unbox = TRUE)
  expect_forbidden("POST", sprintf("%s/objects/link", project_path), link_body)
})

test_that("storage upload endpoints are admin-only", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  auth_header <- get_auth_header("bearer", release_env$token)
  tmp <- tempfile(fileext = ".parquet")
  writeLines("dummy", tmp)

  # Parquet upload
  url <- build_url(sprintf("storage/projects/%s/objects", release_env$project1))
  response <- httr::POST(url,
    body = list(
      file = httr::upload_file(tmp),
      object = "test/should-not-upload.parquet"
    ),
    httr::add_headers(auth_header)
  )
  expect_equal(httr::status_code(response), 403)

  # CSV upload
  csv_tmp <- tempfile(fileext = ".csv")
  writeLines("col1,col2\na,b", csv_tmp)
  csv_url <- build_url(sprintf("storage/projects/%s/csv", release_env$project1))
  response <- httr::POST(csv_url,
    body = list(
      file = httr::upload_file(csv_tmp),
      object = "test/should-not-upload.parquet",
      numberOfRowsToDetermineTypeBy = "100"
    ),
    httr::add_headers(auth_header)
  )
  expect_equal(httr::status_code(response), 403)

  unlink(c(tmp, csv_tmp))
})

test_that("all-workspaces endpoint is admin-only", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  expect_forbidden("GET", "all-workspaces")
})

# ---- Researcher restricted to project ----
# These endpoints allow project-level access (ROLE_{PROJECT}_RESEARCHER).
# A researcher with access to project1 should be denied access to project2.

test_that("setup: create project2 for cross-project tests", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")

  release_env$security_project2 <- generate_random_project_name()

  # Elevate to admin, create project, upload table
  set_user(TRUE, list(release_env$project1))
  armadillo.create_project(release_env$security_project2)
  release_env$created_projects <- c(release_env$created_projects, release_env$security_project2)
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

test_that("researcher cannot load table from project without permission", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test researcher restrictions as admin")
  skip_if(is.null(release_env$security_project2), "Setup failed")

  full_table <- sprintf("%s/test/mtcars", release_env$security_project2)
  url <- build_url("load-table")
  auth_header <- get_auth_header("bearer", release_env$token)
  response <- httr::POST(url,
    query = list(symbol = "tbl_denied", table = full_table),
    httr::add_headers(auth_header)
  )
  expect_equal(httr::status_code(response), 403)
})

