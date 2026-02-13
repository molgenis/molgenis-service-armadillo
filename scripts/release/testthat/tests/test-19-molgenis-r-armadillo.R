library(purrr)

# Setup
test_name <- "molgenis-r-armadillo"

# ---- Project and data listing ----

test_that("list projects", {
  do_skip_test(test_name)
  set_dm_permissions()
  projects <- armadillo.list_projects()
  expect_true(release_env$project1 %in% projects)
})

test_that("load table", {
  do_skip_test(test_name)
  trimesterrep <- armadillo.load_table(
    release_env$project1, "2_1-core-1_0", "trimesterrep"
  )
  expect_s3_class(trimesterrep, "data.frame")
  expect_equal(nrow(trimesterrep), 3000)
  cols <- c("row_id", "child_id", "age_trimester", "smk_t", "alc_t")
  expect_identical(colnames(trimesterrep), cols)
})

test_that("list tables", {
  do_skip_test(test_name)
  tables <- armadillo.list_tables(release_env$project1)
  p <- release_env$project1
  expected_tables <- c(
    sprintf("%s/2_1-core-1_0/nonrep", p),
    sprintf("%s/2_1-core-1_0/yearlyrep", p),
    sprintf("%s/2_1-core-1_0/monthlyrep", p),
    sprintf("%s/2_1-core-1_0/trimesterrep", p),
    sprintf("%s/1_1-outcome-1_0/nonrep", p),
    sprintf("%s/1_1-outcome-1_0/yearlyrep", p),
    sprintf("%s/survival/veteran", p),
    sprintf("%s/tidyverse/mtcars", p),
    sprintf("%s/tidyverse/mtcars_group", p)
  )
  for (et in expected_tables) {
    expect_true(et %in% tables, info = sprintf("Expected table '%s' not found", et))
  }
})

test_that("list resources", {
  skip_if_no_resources(test_name)
  resources <- armadillo.list_resources(release_env$project1)
  expected <- sprintf("%s/ewas/GSE66351_1", release_env$project1)
  expect_true(expected %in% resources)
})

# ---- Project info ----

test_that("get projects info", {
  do_skip_test(test_name)
  info <- armadillo.get_projects_info()
  expect_type(info, "list")
  first_entry <- info[[1]]
  expect_type(first_entry$name, "character")
  expect_type(first_entry$users, "list")
  project_names <- sapply(info, function(x) x$name)
  expect_true(release_env$project1 %in% project_names)
})

test_that("get project users", {
  do_skip_test(test_name)
  users <- armadillo.get_project_users(release_env$project1)
  expect_type(users, "list")
  expect_match(users[[1]], "@", fixed = TRUE)
})

# ---- Copy and move tables ----

test_that("copy table", {
  do_skip_test(test_name)
  armadillo.copy_table(
    project = release_env$project1,
    folder = "2_1-core-1_0",
    name = "nonrep",
    new_folder = "armtest",
    new_name = "nonrep_copy"
  )
  tables <- armadillo.list_tables(release_env$project1)
  expected <- sprintf("%s/armtest/nonrep_copy", release_env$project1)
  expect_true(expected %in% tables)
  original <- sprintf("%s/2_1-core-1_0/nonrep", release_env$project1)
  expect_true(original %in% tables)
  copied <- armadillo.load_table(release_env$project1, "armtest", "nonrep_copy")
  orig <- armadillo.load_table(release_env$project1, "2_1-core-1_0", "nonrep")
  expect_s3_class(copied, "data.frame")
  expect_identical(colnames(copied), colnames(orig))
  expect_equal(nrow(copied), nrow(orig))
})

test_that("move table", {
  do_skip_test(test_name)
  armadillo.move_table(
    project = release_env$project1,
    folder = "armtest",
    name = "nonrep_copy",
    new_folder = "armtest",
    new_name = "nonrep_moved"
  )
  tables <- armadillo.list_tables(release_env$project1)
  old <- sprintf("%s/armtest/nonrep_copy", release_env$project1)
  new <- sprintf("%s/armtest/nonrep_moved", release_env$project1)
  expect_false(old %in% tables)
  expect_true(new %in% tables)
  moved <- armadillo.load_table(release_env$project1, "armtest", "nonrep_moved")
  expect_s3_class(moved, "data.frame")
  expect_gt(nrow(moved), 0)
  expect_error(
    armadillo.load_table(release_env$project1, "armtest", "nonrep_copy")
  )
})

# ---- Copy and move resources ----

test_that("copy resource", {
  skip_if_no_resources(test_name)
  armadillo.copy_resource(
    project = release_env$project1,
    folder = "ewas",
    name = "GSE66351_1",
    new_folder = "armtest_res",
    new_name = "GSE66351_1_copy"
  )
  resources <- armadillo.list_resources(release_env$project1)
  expected <- sprintf("%s/armtest_res/GSE66351_1_copy", release_env$project1)
  expect_true(expected %in% resources)
  original <- sprintf("%s/ewas/GSE66351_1", release_env$project1)
  expect_true(original %in% resources)
  copied_res <- armadillo.load_resource(release_env$project1, "armtest_res", "GSE66351_1_copy")
  expect_s3_class(copied_res, "resource")
  expect_equal(copied_res$name, "GSE66351_1")
  expect_match(copied_res$url, "gse66351_1\\.rda$")
  expect_equal(copied_res$format, "ExpressionSet")
})

test_that("move resource", {
  skip_if_no_resources(test_name)
  armadillo.move_resource(
    project = release_env$project1,
    folder = "armtest_res",
    name = "GSE66351_1_copy",
    new_folder = "armtest_res",
    new_name = "GSE66351_1_moved"
  )
  resources <- armadillo.list_resources(release_env$project1)
  old <- sprintf("%s/armtest_res/GSE66351_1_copy", release_env$project1)
  new <- sprintf("%s/armtest_res/GSE66351_1_moved", release_env$project1)
  expect_false(old %in% resources)
  expect_true(new %in% resources)
  moved_res <- armadillo.load_resource(release_env$project1, "armtest_res", "GSE66351_1_moved")
  expect_s3_class(moved_res, "resource")
  expect_equal(moved_res$name, "GSE66351_1")
  expect_error(
    armadillo.load_resource(release_env$project1, "armtest_res", "GSE66351_1_copy")
  )
})

# ---- Subsetting ----
test_that("subset", {
  do_skip_test(test_name)
  result <- armadillo.subset(
    input_source = "arguments",
    source_project = release_env$project1,
    source_folder = "2_1-core-1_0",
    source_table = "trimesterrep",
    target_project = release_env$project1,
    target_folder = "armtest_sub",
    target_table = "trimesterrep_sub",
    target_vars = "child_id,smk_t,alc_t"
  )
  tables <- armadillo.list_tables(release_env$project1)
  expected <- sprintf("%s/armtest_sub/trimesterrep_sub", release_env$project1)
  expect_true(expected %in% tables)
  subset_table <- armadillo.load_table(
    release_env$project1, "armtest_sub", "trimesterrep_sub"
  )
  expect_s3_class(subset_table, "data.frame")
  expect_identical(colnames(subset_table), c("child_id", "smk_t", "alc_t"))
  expect_equal(nrow(subset_table), 3000)
})

# ---- Cleanup ----

test_that("delete resource", {
  skip_if_no_resources(test_name)
  armadillo.delete_resource(
    release_env$project1, "armtest_res", "GSE66351_1_moved"
  )
  resources <- armadillo.list_resources(release_env$project1)
  deleted <- sprintf("%s/armtest_res/GSE66351_1_moved", release_env$project1)
  expect_false(deleted %in% resources)
  original <- sprintf("%s/ewas/GSE66351_1", release_env$project1)
  expect_true(original %in% resources)
})

test_that("delete project folders", {
  do_skip_test(test_name)
  folders <- c("armtest", "armtest_res", "armtest_sub")
  for (folder in folders) {
    tryCatch(
      armadillo.delete_project_folder(release_env$project1, folder),
      error = function(e) NULL
    )
  }
  tables <- armadillo.list_tables(release_env$project1)
  for (folder in folders) {
    pattern <- sprintf("%s/%s/", release_env$project1, folder)
    matching <- grep(pattern, tables, fixed = TRUE, value = TRUE)
    expect_length(matching, 0)
  }
})

test_that("delete tables", {
  do_skip_test(test_name)
  expect_no_error({
    armadillo.delete_table(release_env$project1, "2_1-core-1_0", "nonrep")
    armadillo.delete_table(release_env$project1, "2_1-core-1_0", "yearlyrep")
    armadillo.delete_table(release_env$project1, "2_1-core-1_0", "trimesterrep")
    armadillo.delete_table(release_env$project1, "2_1-core-1_0", "monthlyrep")
    armadillo.delete_table(release_env$project1, "1_1-outcome-1_0", "nonrep")
    armadillo.delete_table(release_env$project1, "1_1-outcome-1_0", "yearlyrep")
  })
  tables <- armadillo.list_tables(release_env$project1)
  p <- release_env$project1
  deleted_tables <- c(
    sprintf("%s/2_1-core-1_0/nonrep", p),
    sprintf("%s/2_1-core-1_0/yearlyrep", p),
    sprintf("%s/2_1-core-1_0/trimesterrep", p),
    sprintf("%s/2_1-core-1_0/monthlyrep", p),
    sprintf("%s/1_1-outcome-1_0/nonrep", p),
    sprintf("%s/1_1-outcome-1_0/yearlyrep", p)
  )
  for (dt in deleted_tables) {
    expect_false(dt %in% tables, info = sprintf("Table '%s' should have been deleted", dt))
  }
  expect_true(sprintf("%s/tidyverse/mtcars", p) %in% tables)
  expect_true(sprintf("%s/survival/veteran", p) %in% tables)
})

test_that("delete project", {
  do_skip_test(test_name)
  skip_if(!release_env$interactive, "Skipping interactive test")
  cat(sprintf("\nVerify in UI all data from [%s] is gone.", release_env$project1))
  wait_for_input(release_env$interactive)
  expect_no_error(armadillo.delete_project(release_env$project1))
  wait_for_input(release_env$interactive)
})
