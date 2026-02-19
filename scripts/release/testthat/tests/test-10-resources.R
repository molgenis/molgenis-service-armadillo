# Setup
test_name <- "resources"

# Helper: reconnect DataSHIELD session (failed operations can crash the R session)
reconnect <- function() {
  tryCatch(datashield.logout(release_env$conns), error = function(e) NULL)
  logindata <- suppressWarnings(create_dsi_builder())
  release_env$conns <- datashield.login(logins = logindata, assign = FALSE)
}

# ---- Cross-project resource permissions ----
#
# These tests verify the resource security model. When a resource is loaded,
# Armadillo:
#   1. Reads the resource descriptor (.rds) from the researcher's project
#   2. Extracts the URL of the actual data file from the descriptor
#   3. Generates a short-lived internal JWT scoped to that file
#   4. R server uses the internal token to download via /rawfiles/
#
# Each condition is tested with endpoint:
#   - /objects/  endpoint: /storage/projects/{project}/objects/{folder}%2F{file}
#
# Researcher has access to project-a only. project-b is inaccessible.

# Grant researcher access to project-a (not project-b)
set_user(FALSE, list(release_env$project1, release_env$res_project_a))

test_that("researcher can see resources in accessible project", {
  skip_if_no_resources(test_name)

  all_resources <- datashield.resources(conns = release_env$conns)$armadillo
  project_a_resources <- grep(release_env$res_project_a, all_resources, value = TRUE)

  expected <- sprintf("%s/ewas/%s", release_env$res_project_a,
    c("new_i", "new_iii", "old_i", "old_iii"))
  expect_setequal(project_a_resources, expected)
})

test_that("researcher cannot see resources in inaccessible project", {
  skip_if_no_resources(test_name)

  all_resources <- datashield.resources(conns = release_env$conns)$armadillo
  project_b_resources <- grep(release_env$res_project_b, all_resources, value = TRUE)

  expect_length(project_b_resources, 0)
})

test_that("/objects/ (i): resource file and object in same project, has access - assign and resolve succeed", {
  skip_if_no_resources(test_name)

  resource_path <- sprintf("%s/ewas/old_i", release_env$res_project_a)
  expect_no_error(
    datashield.assign.resource(release_env$conns, resource = resource_path, symbol = "old_i")
  )
  symbols <- ds.ls(datasources = release_env$conns)
  expect_true("old_i" %in% symbols$armadillo$objects.found)

  datashield.assign.expr(
    release_env$conns,
    symbol = "old_i_resolved",
    expr = as.symbol("as.resource.object(old_i)")
  )
  symbols <- ds.ls(datasources = release_env$conns)
  expect_true("old_i_resolved" %in% symbols$armadillo$objects.found,
    info = "/objects/ resource in same accessible project should resolve")
})
reconnect()

test_that("/objects/ (ii): resource file and object in same project, no access - assign fails", {
  skip_if_no_resources(test_name)

  resource_path <- sprintf("%s/ewas/old_ii", release_env$res_project_b)
  # TODO: should be "Access Denied" once ArmadilloResult.R is fixed
  expect_error(
    datashield.assign.resource(release_env$conns, resource = resource_path, symbol = "old_ii"),
    "\\$ operator is invalid for atomic vectors"
  )
})
reconnect()

test_that("/objects/ (iii): resource file accessible, data inaccessible - assign succeeds, resolve fails", {
  skip_if_no_resources(test_name)

  resource_path <- sprintf("%s/ewas/old_iii", release_env$res_project_a)
  expect_no_error(
    datashield.assign.resource(release_env$conns, resource = resource_path, symbol = "old_iii")
  )
  symbols <- ds.ls(datasources = release_env$conns)
  expect_true("old_iii" %in% symbols$armadillo$objects.found,
    info = "Descriptor in accessible project should be assignable")

  expect_error(
    datashield.assign.expr(
      release_env$conns,
      symbol = "old_iii_resolved",
      expr = as.symbol("as.resource.object(old_iii)")
    ),
    "invalid first argument",
    info = "/objects/: researcher must have access to data project to resolve"
  )
})
reconnect()

test_that("/objects/ (iv): resource file inaccessible, data accessible - assign fails", {
  skip_if_no_resources(test_name)

  resource_path <- sprintf("%s/ewas/old_iv", release_env$res_project_b)
  # TODO: should be "Access Denied" once ArmadilloResult.R is fixed
  expect_error(
    datashield.assign.resource(release_env$conns, resource = resource_path, symbol = "old_iv"),
    "\\$ operator is invalid for atomic vectors"
  )
})
reconnect()

# ---- Cross-project with access to both ----
#
# Researcher now has access to both project-a and project-b.
# Re-test the cross-project case: descriptor in project-a, data in project-b.

set_user(FALSE, list(release_env$project1, release_env$res_project_a, release_env$res_project_b))

test_that("/objects/ (v): resource file in project-a, data in project-b, has access to both - assign and resolve succeed", {
  skip_if_no_resources(test_name)

  resource_path <- sprintf("%s/ewas/old_iii", release_env$res_project_a)
  expect_no_error(
    datashield.assign.resource(release_env$conns, resource = resource_path, symbol = "old_v")
  )
  symbols <- ds.ls(datasources = release_env$conns)
  expect_true("old_v" %in% symbols$armadillo$objects.found)

  datashield.assign.expr(
    release_env$conns,
    symbol = "old_v_resolved",
    expr = as.symbol("as.resource.object(old_v)")
  )
  symbols <- ds.ls(datasources = release_env$conns)
  expect_true("old_v_resolved" %in% symbols$armadillo$objects.found,
    info = "Cross-project resource should resolve when researcher has access to both projects")
})
reconnect()

# ---- Cleanup ----

test_that("cleanup: delete projects for resource permission tests", {
  skip_if_no_resources(test_name)

  set_user(TRUE, list(release_env$project1))

  if (!is.null(release_env$res_project_a)) {
    armadillo.delete_project(release_env$res_project_a)
  }
  if (!is.null(release_env$res_project_b)) {
    armadillo.delete_project(release_env$res_project_b)
  }

  set_user(FALSE, list(release_env$project1))

  succeed()
})
