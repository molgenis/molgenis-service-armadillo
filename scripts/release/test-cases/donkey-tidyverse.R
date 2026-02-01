library(dsTidyverseClient)

assign_tidyverse_data <- function(data_path) {
  cli_alert_info(sprintf("Assigning table: [%s%s/mtcars]", release_env$project1, data_path))
  datashield.assign.table(release_env$conns, "mtcars", sprintf("%s%s/mtcars", release_env$project1, data_path))
  cli_alert_info(sprintf("Assigning table: [%s%s/mtcars_group]", release_env$project1, data_path))
  datashield.assign.table(release_env$conns, "mtcars_group", sprintf("%s%s/mtcars_group", release_env$project1, data_path))
}

verify_arrange <- function() {
  cli_alert_info("Checking ds.arrange")
  ds.arrange(
    df.name = "mtcars",
    tidy_expr = list(cyl),
    newobj = "ordered_df",
    datasources = release_env$conns
    )

  res <- ds.class("ordered_df", datasources = release_env$conns)[[1]]
  expect_identical(res, "data.frame")
}

verify_as_tibble <- function() {
  cli_alert_info("Checking ds.as_tibble")
  ds.as_tibble(
    x = "mtcars",
    newobj = "mtcars_tib",
    datasources = release_env$conns
  )

  res <- ds.class("mtcars_tib", datasources = release_env$conns)[[1]]
  expect_identical(res, c("tbl_df", "tbl", "data.frame"))
}

verify_bind_cols <- function() {
  cli_alert_info("Checking ds.bind_cols")
  ds.bind_cols(
    to_combine = list(mtcars, mtcars),
    newobj = "cols_bound",
    datasources = release_env$conns
  )

  res <- ds.dim("cols_bound", datasources = release_env$conns)[[1]]
  expect_identical(res, as.integer(c(32, 22)))
}

verify_bind_rows <- function() {
  cli_alert_info("Checking ds.bind_rows")
  ds.bind_rows(
    to_combine = list(mtcars, mtcars),
    newobj = "rows_bound",
    datasources = release_env$conns
  )

  res <- ds.dim("rows_bound", datasources = release_env$conns)[[1]]
  expect_identical(res, as.integer(c(64, 11)))
}

verify_case_when <- function() {
  cli_alert_info("Checking ds.case_when")
  ds.case_when(
    tidy_expr = list(
      mtcars$mpg < 20 ~ "low",
      mtcars$mpg >= 20 & mtcars$mpg < 30 ~ "medium",
      mtcars$mpg >= 30 ~ "high"
    ),
    newobj = "test",
    datasources = release_env$conns
  )

  res <- names(ds.table("test", datasources = release_env$conns)$output.list$TABLES.COMBINED_all.sources_counts)
  expect_identical(res, c("high", "low", "medium", "NA"))
}

verify_distinct <- function() {
  cli_alert_info("Checking ds.distinct")
  ds.distinct(
    df.name = "mtcars",
    tidy_expr = list(cyl, carb),
    newobj = "dist_df",
    datasources = release_env$conns
  )

  res <- ds.dim("dist_df", datasources = release_env$conns)[[1]]
  expect_identical(res, as.integer(c(9, 2)))
}

verify_filter <- function() {
  cli_alert_info("Checking ds.filter")
  ds.filter(
    df.name = "mtcars",
    tidy_expr = list(cyl == 4 & mpg > 20),
    newobj = "filtered",
    datasources = release_env$conns
  )

  res <- ds.dim("filtered", datasources = release_env$conns)[[1]]
  expect_identical(res, as.integer(c(11, 11)))
}

verify_group_by <- function() {
  cli_alert_info("Checking ds.group_by")
  ds.group_by(
    df.name = "mtcars",
    tidy_expr = list(cyl),
    newobj = "grouped",
    datasources = release_env$conns
  )

  res <- ds.class("grouped", datasources = release_env$conns)[[1]]
  expect_identical(res, c("grouped_df", "tbl_df", "tbl", "data.frame"))
}

verify_ungroup <- function() {
  cli_alert_info("Checking ds.ungroup")
  ds.ungroup("grouped", "ungrouped_df", datasources = release_env$conns)
  res <- ds.class("ungrouped_df", datasources = release_env$conns)[[1]]
  expect_identical(res, c("tbl_df", "tbl", "data.frame"))
}

verify_group_keys <- function() {
  cli_alert_info("Checking ds.group_keys")
  res <- ds.group_keys("grouped", datasources = release_env$conns)$armadillo
  expect_identical(res, tibble(cyl = c(4, 6, 8)))
}

verify_if_else <- function() {
  cli_alert_info("Checking ds.if_else")

  ds.if_else(
    condition = list(mtcars$mpg > 20),
    "high",
    "low",
    newobj = "test",
    datasources = release_env$conns
  )

  res <- names(ds.table("test", datasources = release_env$conns)$output.list$TABLES.COMBINED_all.sources_counts)
  expect_identical(res, c("high", "low", "NA"))
}

verify_mutate <- function() {
  cli_alert_info("Checking ds.mutate")

  ds.mutate(
    df.name = "mtcars",
    tidy_expr = list(mpg_trans = cyl * 1000, new_var = (hp - drat) / qsec),
    newobj = "new",
    datasources = release_env$conns
  )

  res <- ds.colnames("new", datasources = release_env$conns)$armadillo
  expect_identical(res, c("mpg", "cyl", "disp", "hp", "drat", "wt", "qsec", "vs", "am", "gear", "carb", "mpg_trans", "new_var"))
}

verify_rename <- function() {
  cli_alert_info("Checking ds.rename")

  ds.rename(
    df.name = "mtcars",
    tidy_expr = list(test_1 = mpg, test_2 = drat),
    newobj = "mpg_drat",
    datasources = release_env$conns
  )
  res <- ds.colnames("mpg_drat", datasources = release_env$conns)$armadillo
  expect_identical(res, c("test_1", "cyl", "disp", "hp", "test_2", "wt", "qsec", "vs", "am", "gear", "carb"))
}

verify_select <- function() {
  cli_alert_info("Checking ds.select")

  ds.select(
    df.name = "mtcars",
    tidy_expr = list(mpg:drat),
    newobj = "mpg_drat",
    datasources = release_env$conns
  )
  res <-  ds.colnames("mpg_drat", datasources = release_env$conns)$armadillo
  expect_identical(res, c("mpg", "cyl", "disp", "hp", "drat"))
}

verify_slice <- function() {
  cli_alert_info("Checking ds.slice")

  ds.slice(
    df.name = "mtcars",
    tidy_expr = list(1:5),
    newobj = "sliced",
    datasources = release_env$conns
  )

  res <-  ds.dim("sliced", datasources = release_env$conns)[[1]]
  expect_identical(res, as.integer(c(5, 11)))
}

run_tidyverse_tests <- function(data_path) {
  test_name <- "donkey-tidyverse"
  test_that("donkey-tidyverse setup", {
    do_skip_test(test_name)
    assign_tidyverse_data(data_path)
    succeed()
  })
  test_that("ds.arrange", { do_skip_test(test_name); verify_arrange() })
  test_that("ds.as_tibble", { do_skip_test(test_name); verify_as_tibble() })
  test_that("ds.bind_cols", { do_skip_test(test_name); verify_bind_cols() })
  test_that("ds.bind_rows", { do_skip_test(test_name); verify_bind_rows() })
  test_that("ds.case_when", { do_skip_test(test_name); verify_case_when() })
  test_that("ds.distinct", { do_skip_test(test_name); verify_distinct() })
  test_that("ds.filter", { do_skip_test(test_name); verify_filter() })
  test_that("ds.group_by", { do_skip_test(test_name); verify_group_by() })
  test_that("ds.ungroup", { do_skip_test(test_name); verify_ungroup() })
  test_that("ds.group_keys", { do_skip_test(test_name); verify_group_keys() })
  test_that("ds.if_else", { do_skip_test(test_name); verify_if_else() })
  test_that("ds.mutate", { do_skip_test(test_name); verify_mutate() })
  test_that("ds.rename", { do_skip_test(test_name); verify_rename() })
  test_that("ds.select", { do_skip_test(test_name); verify_select() })
  test_that("ds.slice", { do_skip_test(test_name); verify_slice() })
}
