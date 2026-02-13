# Setup
test_name <- "dsi"
folder <- "2_1-core-1_0"
table <- "nonrep"

# ---- 1. datashield.tables ----
test_that("datashield.tables returns tables including known table", {
  do_skip_test(test_name)
  tables <- datashield.tables(conns = release_env$conns)
  cat("\n--- datashield.tables ---\n")
  cat("class:", class(tables), "\n")
  cat("names:", names(tables), "\n")
  cat("tables$armadillo:\n")
  str(tables$armadillo)
  expect_type(tables, "list")
  expect_true("armadillo" %in% names(tables))
  full_table <- sprintf("%s/%s/%s", release_env$project1, folder, table)
  expect_true(full_table %in% tables$armadillo)
})

# ---- 2. datashield.table_status ----
test_that("datashield.table_status shows accessible for existing table", {
  do_skip_test(test_name)
  full_table <- sprintf("%s/%s/%s", release_env$project1, folder, table)
  status <- datashield.table_status(conns = release_env$conns, table = full_table)
  cat("\n--- datashield.table_status (existing) ---\n")
  cat("class:", class(status), "\n")
  cat("colnames:", colnames(status), "\n")
  print(status)
  expect_s3_class(status, "data.frame")
  expect_equal(colnames(status), c("server", "table", "accessible"))
  expect_equal(nrow(status), 1)
  expect_equal(status$server, "armadillo")
  expect_equal(status$table, full_table)
  expect_true(status$accessible)
})

test_that("datashield.table_status shows not accessible for nonexistent table", {
  do_skip_test(test_name)
  fake_table <- "fake_project/fake_folder/fake_table"
  status <- datashield.table_status(conns = release_env$conns, table = fake_table)
  cat("\n--- datashield.table_status (nonexistent) ---\n")
  print(status)
  expect_s3_class(status, "data.frame")
  expect_equal(nrow(status), 1)
  expect_equal(status$server, "armadillo")
  expect_equal(status$table, fake_table)
  expect_false(status$accessible)
})

# ---- 3. datashield.resource_status ----
test_that("datashield.resource_status shows accessible for existing resource", {
  skip_if_no_resources(test_name)
  resource_path <- sprintf("%s/ewas/GSE66351_1", release_env$project1)
  status <- datashield.resource_status(conns = release_env$conns, resource = resource_path)
  cat("\n--- datashield.resource_status ---\n")
  cat("class:", class(status), "\n")
  cat("colnames:", colnames(status), "\n")
  print(status)
  expect_s3_class(status, "data.frame")
  expect_equal(colnames(status), c("server", "resource", "accessible"))
  expect_equal(nrow(status), 1)
  expect_equal(status$server, "armadillo")
  expect_equal(status$resource, resource_path)
  expect_true(status$accessible)
})

# ---- 4. datashield.profiles ----
test_that("datashield.profiles returns available and current profiles", {
  do_skip_test(test_name)
  profiles <- datashield.profiles(conns = release_env$conns)
  cat("\n--- datashield.profiles ---\n")
  cat("class:", class(profiles), "\n")
  cat("names:", names(profiles), "\n")
  cat("$available:\n")
  print(profiles$available)
  cat("$current:\n")
  print(profiles$current)
  expect_type(profiles, "list")
  expect_true(all(c("available", "current") %in% names(profiles)))
  expect_s3_class(profiles$available, "data.frame")
  expect_true("default" %in% rownames(profiles$available))
  expect_s3_class(profiles$current, "data.frame")
  expect_true("profile" %in% colnames(profiles$current))
})

# ---- 5. datashield.methods ----
test_that("datashield.methods returns aggregate methods including meanDS", {
  do_skip_test(test_name)
  methods <- datashield.methods(conns = release_env$conns, type = "aggregate")
  cat("\n--- datashield.methods (aggregate) ---\n")
  cat("class:", class(methods), "\n")
  cat("colnames:", colnames(methods), "\n")
  cat("nrow:", nrow(methods), "\n")
  cat("first 10 rows:\n")
  print(head(methods, 10))
  expected_cols <- c("name", "function.", "version", "package", "type", "class", "server")
  expect_s3_class(methods, "data.frame")
  expect_equal(colnames(methods), expected_cols)
  expect_true(nrow(methods) > 0)
  expect_true("meanDS" %in% methods$name)
  expect_true(all(methods$type == "aggregate"))
})

test_that("datashield.methods returns assign methods", {
  do_skip_test(test_name)
  methods <- datashield.methods(conns = release_env$conns, type = "assign")
  cat("\n--- datashield.methods (assign) ---\n")
  cat("class:", class(methods), "\n")
  cat("colnames:", colnames(methods), "\n")
  cat("nrow:", nrow(methods), "\n")
  cat("first 10 rows:\n")
  print(head(methods, 10))
  expected_cols <- c("name", "function.", "version", "package", "type", "class", "server")
  expect_s3_class(methods, "data.frame")
  expect_equal(colnames(methods), expected_cols)
  expect_true(nrow(methods) > 0)
  expect_true(all(methods$type == "assign"))
})

# ---- 6. datashield.method_status ----
test_that("datashield.method_status returns boolean status per server", {
  do_skip_test(test_name)
  status <- datashield.method_status(conns = release_env$conns, type = "aggregate")
  cat("\n--- datashield.method_status ---\n")
  cat("class:", class(status), "\n")
  cat("colnames:", colnames(status), "\n")
  cat("nrow:", nrow(status), "\n")
  cat("first 10 rows:\n")
  print(head(status, 10))
  expect_s3_class(status, "data.frame")
  expect_equal(colnames(status), c("name", "type", "armadillo"))
  expect_true(nrow(status) > 0)
  expect_type(status$armadillo, "logical")
})

# ---- 7. datashield.pkg_status ----
test_that("datashield.pkg_status returns package and version status", {
  do_skip_test(test_name)
  pkg <- datashield.pkg_status(conns = release_env$conns)
  cat("\n--- datashield.pkg_status ---\n")
  cat("class:", class(pkg), "\n")
  cat("names:", names(pkg), "\n")
  cat("$package_status:\n")
  print(pkg$package_status)
  cat("$version_status:\n")
  print(pkg$version_status)
  expect_type(pkg, "list")
  expect_equal(names(pkg), c("package_status", "version_status"))
  # package_status is a named logical matrix with one column per server
  expect_true(is.matrix(pkg$package_status))
  expect_true("armadillo" %in% colnames(pkg$package_status))
  expect_type(pkg$package_status[, "armadillo"], "logical")
  expect_true("dsBase" %in% rownames(pkg$package_status))
  expect_true(pkg$package_status["dsBase", "armadillo"])
  # version_status is a named character matrix with one column per server
  expect_true(is.matrix(pkg$version_status))
  expect_true("armadillo" %in% colnames(pkg$version_status))
  expect_true("dsBase" %in% rownames(pkg$version_status))
  expect_type(pkg$version_status["dsBase", "armadillo"], "character")
})

# ---- 8. datashield.pkg_check ----
test_that("datashield.pkg_check passes for dsBase with low version", {
  do_skip_test(test_name)
  # DSI bug: pkg_check uses $ on named vector when there's a single server,
  # causing "$ operator is invalid for atomic vectors". Skip until DSI is fixed.
  skip("DSI bug: pkg_check fails with single server ($ on atomic vector)")
  cat("\n--- datashield.pkg_check (dsBase >= 0.0.1) ---\n")
  result <- datashield.pkg_check(conns = release_env$conns, name = "dsBase", version = "0.0.1")
  cat("result:", class(result), "\n")
  print(result)
  expect_no_error(datashield.pkg_check(conns = release_env$conns, name = "dsBase", version = "0.0.1"))
})

test_that("datashield.pkg_check errors for nonexistent package", {
  do_skip_test(test_name)
  skip("DSI bug: pkg_check fails with single server ($ on atomic vector)")
  cat("\n--- datashield.pkg_check (nonExistentPkg) ---\n")
  err <- tryCatch(
    datashield.pkg_check(conns = release_env$conns, name = "nonExistentPkg", version = "1.0.0"),
    error = function(e) e
  )
  cat("error message:", conditionMessage(err), "\n")
  expect_error(datashield.pkg_check(conns = release_env$conns, name = "nonExistentPkg", version = "1.0.0"))
})

# ---- 9. datashield.symbols ----
test_that("datashield.symbols lists assigned symbols", {
  do_skip_test(test_name)
  full_table <- sprintf("%s/%s/%s", release_env$project1, folder, table)
  datashield.assign.table(release_env$conns, "dsi_test_df", full_table)
  symbols <- datashield.symbols(conns = release_env$conns)
  cat("\n--- datashield.symbols ---\n")
  cat("class:", class(symbols), "\n")
  cat("names:", names(symbols), "\n")
  str(symbols)
  expect_type(symbols, "list")
  expect_true("armadillo" %in% names(symbols))
  expect_true("dsi_test_df" %in% symbols$armadillo)
})

# ---- 10. datashield.rm ----
test_that("datashield.rm removes a symbol", {
  do_skip_test(test_name)
  full_table <- sprintf("%s/%s/%s", release_env$project1, folder, table)
  datashield.assign.table(release_env$conns, "dsi_temp_symbol", full_table)
  symbols_before <- datashield.symbols(conns = release_env$conns)
  cat("\n--- datashield.rm ---\n")
  cat("symbols before:", symbols_before$armadillo, "\n")
  expect_true("dsi_temp_symbol" %in% symbols_before$armadillo)

  rm_result <- datashield.rm(conns = release_env$conns, symbol = "dsi_temp_symbol")
  cat("rm result class:", class(rm_result), "\n")
  cat("rm result:\n")
  print(rm_result)
  symbols_after <- datashield.symbols(conns = release_env$conns)
  cat("symbols after:", symbols_after$armadillo, "\n")
  expect_false("dsi_temp_symbol" %in% symbols_after$armadillo)
})

# ---- 11. datashield.aggregate ----
test_that("datashield.aggregate returns result (async)", {
  do_skip_test(test_name)
  result <- datashield.aggregate(conns = release_env$conns, expr = quote(classDS("dsi_test_df")))
  cat("\n--- datashield.aggregate (async) ---\n")
  cat("class:", class(result), "\n")
  cat("names:", names(result), "\n")
  str(result)
  expected <- list(armadillo = "data.frame")
  expect_identical(result, expected)
})

test_that("datashield.aggregate returns result (sync)", {
  do_skip_test(test_name)
  result <- datashield.aggregate(conns = release_env$conns, expr = quote(classDS("dsi_test_df")), async = FALSE)
  cat("\n--- datashield.aggregate (sync) ---\n")
  cat("class:", class(result), "\n")
  cat("names:", names(result), "\n")
  str(result)
  expected <- list(armadillo = "data.frame")
  expect_identical(result, expected)
})

# ---- 12. Workspace lifecycle ----
test_that("workspace save, list, restore, and remove lifecycle", {
  do_skip_test(test_name)
  ws_name <- paste0("dsi_test_ws_", format(Sys.time(), "%Y%m%d%H%M%S"))
  prefixed_name <- paste0("armadillo:", ws_name)
  cat("\n--- workspace lifecycle ---\n")
  cat("ws_name:", ws_name, "\n")
  cat("prefixed_name:", prefixed_name, "\n")

  # Save
  save_result <- datashield.workspace_save(conns = release_env$conns, ws = ws_name)
  cat("save result class:", class(save_result), "\n")
  cat("save result:\n")
  print(save_result)

  # List and verify saved workspace appears
  workspaces <- datashield.workspaces(conns = release_env$conns)
  cat("workspaces class:", class(workspaces), "\n")
  cat("workspaces colnames:", colnames(workspaces), "\n")
  print(workspaces)
  expect_s3_class(workspaces, "data.frame")
  expect_equal(colnames(workspaces), c("server", "name", "user", "lastAccessDate", "size"))
  expect_true(prefixed_name %in% workspaces$name,
    info = sprintf("Workspace '%s' not found. Available: %s",
                   prefixed_name, paste(workspaces$name, collapse = ", ")))

  # Restore — dsRestoreWorkspace is not implemented for ArmadilloConnection
  # so we skip this step

  # Remove
  rm_result <- datashield.workspace_rm(conns = release_env$conns, ws = ws_name)
  cat("rm result class:", class(rm_result), "\n")
  cat("rm result:\n")
  print(rm_result)

  # Verify gone
  workspaces_after <- datashield.workspaces(conns = release_env$conns)
  cat("workspaces after removal:\n")
  print(workspaces_after)
  if (is.data.frame(workspaces_after) && nrow(workspaces_after) > 0) {
    expect_false(prefixed_name %in% workspaces_after$name)
  } else {
    succeed()
  }
})

# ---- 13. Cleanup ----
test_that("cleanup dsi test symbols", {
  do_skip_test(test_name)
  expect_no_error(datashield.rm(conns = release_env$conns, symbol = "dsi_test_df"))
})
