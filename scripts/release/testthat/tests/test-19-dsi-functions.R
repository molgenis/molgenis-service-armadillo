# Setup
test_name <- "dsi"
folder <- "2_1-core-1_0"
table <- "nonrep"

# ---- 1. datashield.tables ----
test_that("datashield.tables returns tables including known table", {
  do_skip_test(test_name)
  tables <- datashield.tables(conns = release_env$conns)
  expect_true("armadillo" %in% names(tables))
  full_table <- sprintf("%s/%s/%s", release_env$project1, folder, table)
  expect_true(full_table %in% tables$armadillo)
})

# ---- 2. datashield.table_status ----
test_that("datashield.table_status shows accessible for existing table", {
  do_skip_test(test_name)
  full_table <- sprintf("%s/%s/%s", release_env$project1, folder, table)
  status <- datashield.table_status(conns = release_env$conns, table = full_table)
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
  expect_false(status$accessible)
})

# ---- 3. datashield.resource_status ----
test_that("datashield.resource_status shows accessible for existing resource", {
  skip_if_no_resources(test_name)
  resource_path <- sprintf("%s/ewas/GSE66351_1", release_env$project1)
  status <- datashield.resource_status(conns = release_env$conns, resource = resource_path)
  expect_equal(colnames(status), c("server", "resource", "accessible"))
  expect_equal(nrow(status), 1)
  expect_true(status$accessible)
})

# ---- 4. datashield.profiles ----
test_that("datashield.profiles returns available and current profiles", {
  do_skip_test(test_name)
  profiles <- datashield.profiles(conns = release_env$conns)
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
  expected_cols <- c("name", "function.", "version", "package", "type", "class", "server")
  expect_equal(colnames(methods), expected_cols)
  expect_true("meanDS" %in% methods$name)
  expect_true(all(methods$type == "aggregate"))
})

test_that("datashield.methods returns assign methods", {
  do_skip_test(test_name)
  methods <- datashield.methods(conns = release_env$conns, type = "assign")
  expected_cols <- c("name", "function.", "version", "package", "type", "class", "server")
  expect_equal(colnames(methods), expected_cols)
  expect_true(nrow(methods) > 0)
  expect_true(all(methods$type == "assign"))
})

# ---- 6. datashield.method_status ----
test_that("datashield.method_status returns boolean status per server", {
  do_skip_test(test_name)
  status <- datashield.method_status(conns = release_env$conns, type = "aggregate")
  expect_equal(colnames(status), c("name", "type", "armadillo"))
  expect_true(nrow(status) > 0)
  expect_type(status$armadillo, "logical")
})

# ---- 7. datashield.pkg_status ----
test_that("datashield.pkg_status returns package and version status", {
  do_skip_test(test_name)
  pkg <- datashield.pkg_status(conns = release_env$conns)
  expect_equal(names(pkg), c("package_status", "version_status"))
  # package_status is a named logical matrix with one column per server
  expect_true(is.matrix(pkg$package_status))
  expect_true(pkg$package_status["dsBase", "armadillo"])
  # version_status is a named character matrix with one column per server
  expect_true(is.matrix(pkg$version_status))
  expect_type(pkg$version_status["dsBase", "armadillo"], "character")
})

# ---- 8. datashield.symbols ----
test_that("datashield.symbols lists assigned symbols", {
  do_skip_test(test_name)
  full_table <- sprintf("%s/%s/%s", release_env$project1, folder, table)
  datashield.assign.table(release_env$conns, "dsi_test_df", full_table)
  symbols <- datashield.symbols(conns = release_env$conns)
  expect_true("armadillo" %in% names(symbols))
  expect_true("dsi_test_df" %in% symbols$armadillo)
})

# ---- 9. datashield.rm ----
test_that("datashield.rm removes a symbol", {
  do_skip_test(test_name)
  full_table <- sprintf("%s/%s/%s", release_env$project1, folder, table)
  datashield.assign.table(release_env$conns, "dsi_temp_symbol", full_table)
  symbols_before <- datashield.symbols(conns = release_env$conns)
  expect_true("dsi_temp_symbol" %in% symbols_before$armadillo)

  datashield.rm(conns = release_env$conns, symbol = "dsi_temp_symbol")
  symbols_after <- datashield.symbols(conns = release_env$conns)
  expect_false("dsi_temp_symbol" %in% symbols_after$armadillo)
})

# ---- 10. datashield.aggregate ----
test_that("datashield.aggregate returns result (async)", {
  do_skip_test(test_name)
  result <- datashield.aggregate(conns = release_env$conns, expr = quote(classDS("dsi_test_df")))
  expected <- list(armadillo = "data.frame")
  expect_identical(result, expected)
})

test_that("datashield.aggregate returns result (sync)", {
  do_skip_test(test_name)
  result <- datashield.aggregate(conns = release_env$conns, expr = quote(classDS("dsi_test_df")), async = FALSE)
  expected <- list(armadillo = "data.frame")
  expect_identical(result, expected)
})

# ---- 11. Workspace lifecycle ----
release_env$ws_name <- paste0("dsi_test_ws_", format(Sys.time(), "%Y%m%d%H%M%S"))

test_that("datashield.workspace_save saves current session", {
  do_skip_test(test_name)
  expect_no_error(datashield.workspace_save(conns = release_env$conns, ws = release_env$ws_name))
})

test_that("datashield.workspaces lists saved workspace", {
  do_skip_test(test_name)
  prefixed_name <- paste0("armadillo:", release_env$ws_name)
  workspaces <- datashield.workspaces(conns = release_env$conns)
  expect_equal(colnames(workspaces), c("server", "name", "user", "lastAccessDate", "size"))
  expect_true(prefixed_name %in% workspaces$name,
    info = sprintf("Workspace '%s' not found. Available: %s",
                   prefixed_name, paste(workspaces$name, collapse = ", ")))
})

test_that("datashield.workspace_restore restores saved objects", {
  do_skip_test(test_name)
  datashield.rm(conns = release_env$conns, symbol = "dsi_test_df")
  expect_no_error(datashield.workspace_restore(conns = release_env$conns, ws = release_env$ws_name))
  symbols <- ds.ls(datasources = release_env$conns)
  expect_true("dsi_test_df" %in% symbols$armadillo$objects.found,
    info = "Restored workspace should contain 'dsi_test_df'")
})

test_that("datashield.workspace_rm removes workspace", {
  do_skip_test(test_name)
  expect_no_error(datashield.workspace_rm(conns = release_env$conns, ws = release_env$ws_name))
  prefixed_name <- paste0("armadillo:", release_env$ws_name)
  workspaces_after <- datashield.workspaces(conns = release_env$conns)
  if (is.data.frame(workspaces_after) && nrow(workspaces_after) > 0) {
    expect_false(prefixed_name %in% workspaces_after$name)
  } else {
    succeed()
  }
})

# ---- 12. Cleanup ----
datashield.rm(conns = release_env$conns, symbol = "dsi_test_df")
