# Setup
test_name <- "permissions-resources"

# ---- Cross-project resource access ----
#
# These tests verify the resource security model introduced in
# spike/resource_security. When a resource is loaded, Armadillo:
#   1. Reads the resource descriptor (.rds) from the researcher's project
#   2. Extracts the URL of the actual data file from the descriptor
#   3. Generates a short-lived internal JWT scoped to that file
#   4. R server uses the internal token to download via /rawfiles/
#
# Each condition is tested with two URL path types:
#   - /objects/  endpoint: /storage/projects/{project}/objects/{folder}%2F{file}
#   - /rawfiles/ endpoint: /storage/projects/{project}/rawfiles/{folder}%2F{file}
#
# Researcher has access to project-a only. project-b is inaccessible.

# Helper: reconnect DataSHIELD session (failed operations can crash the R session)
reconnect <- function() {
  tryCatch(datashield.logout(release_env$conns), error = function(e) NULL)
  logindata <- suppressWarnings(create_dsi_builder())
  release_env$conns <- datashield.login(logins = logindata, assign = FALSE)
}

# Helper: try to assign a resource, return whether the symbol exists afterwards
try_assign_resource <- function(resource_path, symbol) {
  tryCatch({
    datashield.assign.resource(
      release_env$conns, resource = resource_path, symbol = symbol
    )
  }, error = function(e) {
    cat("\n  assign error:", conditionMessage(e), "\n")
    return(FALSE)
  })

  symbols <- ds.ls(datasources = release_env$conns)
  symbol %in% symbols$armadillo$objects.found
}

# Helper: try to resolve an assigned resource, return whether the symbol exists
try_resolve_resource <- function(source_symbol, target_symbol) {
  tryCatch({
    datashield.assign.expr(
      release_env$conns,
      symbol = target_symbol,
      expr = as.symbol(paste0("as.resource.object(", source_symbol, ")"))
    )
    symbols <- ds.ls(datasources = release_env$conns)
    target_symbol %in% symbols$armadillo$objects.found
  }, error = function(e) {
    cat("\n  resolve error:", conditionMessage(e), "\n")
    FALSE
  })
}

# Grant researcher access to project-a (not project-b)
set_user(FALSE, list(release_env$project1, release_env$res_project_a))

# ---- Visibility ----

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

# ---- /objects/ endpoint ----

test_that("/objects/ (i): resource file and object in same project, has access - assign and resolve succeed", {
  skip_if_no_resources(test_name)

  resource_path <- sprintf("%s/ewas/old_i", release_env$res_project_a)
  was_assigned <- try_assign_resource(resource_path, "old_i")
  expect_true(was_assigned)

  was_resolved <- try_resolve_resource("old_i", "old_i_resolved")
  expect_true(was_resolved,
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
  was_assigned <- try_assign_resource(resource_path, "old_iii")
  expect_true(was_assigned,
    info = "Descriptor in accessible project should be assignable")

  was_resolved <- try_resolve_resource("old_iii", "old_iii_resolved")
  expect_false(was_resolved,
    info = "/objects/: researcher must have access to data project to resolve")
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

# ---- /rawfiles/ endpoint ----

test_that("/rawfiles/ (i): resource file and object in same project, has access - assign and resolve succeed", {
  skip_if_no_resources(test_name)

  resource_path <- sprintf("%s/ewas/new_i", release_env$res_project_a)
  was_assigned <- try_assign_resource(resource_path, "new_i")
  expect_true(was_assigned)

  was_resolved <- try_resolve_resource("new_i", "new_i_resolved")
  expect_true(was_resolved,
    info = "/rawfiles/ resource in same accessible project should resolve")
})
reconnect()

test_that("/rawfiles/ (ii): resource file and object in same project, no access - assign fails", {
  skip_if_no_resources(test_name)

  resource_path <- sprintf("%s/ewas/new_ii", release_env$res_project_b)
  # TODO: should be "Access Denied" once ArmadilloResult.R is fixed
  expect_error(
    datashield.assign.resource(release_env$conns, resource = resource_path, symbol = "new_ii"),
    "\\$ operator is invalid for atomic vectors"
  )
})
reconnect()

test_that("/rawfiles/ (iii): resource file accessible, data inaccessible - assign succeeds, resolve fails", {
  skip_if_no_resources(test_name)

  resource_path <- sprintf("%s/ewas/new_iii", release_env$res_project_a)
  was_assigned <- try_assign_resource(resource_path, "new_iii")
  expect_true(was_assigned,
    info = "Descriptor in accessible project should be assignable")

  was_resolved <- try_resolve_resource("new_iii", "new_iii_resolved")
  expect_false(was_resolved,
    info = "/rawfiles/: researcher must have access to data project to resolve")
})
reconnect()

test_that("/rawfiles/ (iv): resource file inaccessible, data accessible - assign fails", {
  skip_if_no_resources(test_name)

  resource_path <- sprintf("%s/ewas/new_iv", release_env$res_project_b)
  # TODO: should be "Access Denied" once ArmadilloResult.R is fixed
  expect_error(
    datashield.assign.resource(release_env$conns, resource = resource_path, symbol = "new_iv"),
    "\\$ operator is invalid for atomic vectors"
  )
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
