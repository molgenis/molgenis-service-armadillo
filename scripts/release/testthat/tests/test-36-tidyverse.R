# test-36-tidyverse.R - dsTidyverse package tests
#
# These tests verify that dsTidyverse functions work correctly.

# Setup: ensure researcher connection is established
ensure_researcher_login()

# Skip all tests if donkey-tidyverse is excluded
skip_if_excluded("donkey-tidyverse")

# Load the tidyverse client library
library(dsTidyverseClient)

# Helper to assign tidyverse test data
assign_tidyverse_data <- function() {
  conns <- test_env$conns
  project <- test_env$project
  data_path <- "/tidyverse"

  DSI::datashield.assign.table(
    conns,
    "mtcars",
    sprintf("%s%s/mtcars", project, data_path)
  )
}

test_that("tidyverse data can be assigned", {
  assign_tidyverse_data()

  conns <- test_env$conns
  datatype <- dsBaseClient::ds.class(x = "mtcars", datasources = conns)

  expect_equal(datatype$armadillo, "data.frame")
})

test_that("ds.arrange creates data frame", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.arrange(
    df.name = "mtcars",
    tidy_expr = list(cyl),
    newobj = "ordered_df",
    datasources = conns
  )

  res <- dsBaseClient::ds.class("ordered_df", datasources = conns)[[1]]

  expect_equal(res, "data.frame")
})

test_that("ds.as_tibble creates tibble", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.as_tibble(
    x = "mtcars",
    newobj = "mtcars_tib",
    datasources = conns
  )

  res <- dsBaseClient::ds.class("mtcars_tib", datasources = conns)[[1]]

  expect_identical(res, c("tbl_df", "tbl", "data.frame"))
})

test_that("ds.bind_cols creates correct dimensions", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.bind_cols(
    to_combine = list(mtcars, mtcars),
    newobj = "cols_bound",
    datasources = conns
  )

  res <- dsBaseClient::ds.dim("cols_bound", datasources = conns)[[1]]

  expect_identical(res, as.integer(c(32, 22)))
})

test_that("ds.bind_rows creates correct dimensions", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.bind_rows(
    to_combine = list(mtcars, mtcars),
    newobj = "rows_bound",
    datasources = conns
  )

  res <- dsBaseClient::ds.dim("rows_bound", datasources = conns)[[1]]

  expect_identical(res, as.integer(c(64, 11)))
})

test_that("ds.case_when creates expected levels", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.case_when(
    tidy_expr = list(
      mtcars$mpg < 20 ~ "low",
      mtcars$mpg >= 20 & mtcars$mpg < 30 ~ "medium",
      mtcars$mpg >= 30 ~ "high"
    ),
    newobj = "test",
    datasources = conns
  )

  res <- names(dsBaseClient::ds.table("test", datasources = conns)$output.list$TABLES.COMBINED_all.sources_counts)

  expect_identical(res, c("high", "low", "medium", "NA"))
})

test_that("ds.distinct creates correct dimensions", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.distinct(
    df.name = "mtcars",
    tidy_expr = list(cyl, carb),
    newobj = "dist_df",
    datasources = conns
  )

  res <- dsBaseClient::ds.dim("dist_df", datasources = conns)[[1]]

  expect_identical(res, as.integer(c(9, 2)))
})

test_that("ds.filter creates correct dimensions", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.filter(
    df.name = "mtcars",
    tidy_expr = list(cyl == 4 & mpg > 20),
    newobj = "filtered",
    datasources = conns
  )

  res <- dsBaseClient::ds.dim("filtered", datasources = conns)[[1]]

  expect_identical(res, as.integer(c(11, 11)))
})

test_that("ds.group_by creates grouped_df", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.group_by(
    df.name = "mtcars",
    tidy_expr = list(cyl),
    newobj = "grouped",
    datasources = conns
  )

  res <- dsBaseClient::ds.class("grouped", datasources = conns)[[1]]

  expect_identical(res, c("grouped_df", "tbl_df", "tbl", "data.frame"))
})

test_that("ds.ungroup removes grouping", {
  assign_tidyverse_data()

  conns <- test_env$conns

  # Ensure grouped exists
  tryCatch({
    dsBaseClient::ds.class("grouped", datasources = conns)
  }, error = function(e) {
    ds.group_by(
      df.name = "mtcars",
      tidy_expr = list(cyl),
      newobj = "grouped",
      datasources = conns
    )
  })

  ds.ungroup("grouped", "ungrouped_df", datasources = conns)

  res <- dsBaseClient::ds.class("ungrouped_df", datasources = conns)[[1]]

  expect_identical(res, c("tbl_df", "tbl", "data.frame"))
})

test_that("ds.group_keys returns expected keys", {
  assign_tidyverse_data()

  conns <- test_env$conns

  # Ensure grouped exists
  tryCatch({
    dsBaseClient::ds.class("grouped", datasources = conns)
  }, error = function(e) {
    ds.group_by(
      df.name = "mtcars",
      tidy_expr = list(cyl),
      newobj = "grouped",
      datasources = conns
    )
  })

  res <- ds.group_keys("grouped", datasources = conns)$armadillo

  expect_equal(res, tibble::tibble(cyl = c(4, 6, 8)))
})

test_that("ds.if_else creates expected levels", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.if_else(
    condition = list(mtcars$mpg > 20),
    "high",
    "low",
    newobj = "test",
    datasources = conns
  )

  res <- names(dsBaseClient::ds.table("test", datasources = conns)$output.list$TABLES.COMBINED_all.sources_counts)

  expect_identical(res, c("high", "low", "NA"))
})

test_that("ds.mutate creates new variables", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.mutate(
    df.name = "mtcars",
    tidy_expr = list(mpg_trans = cyl * 1000, new_var = (hp - drat) / qsec),
    newobj = "new",
    datasources = conns
  )

  res <- dsBaseClient::ds.colnames("new", datasources = conns)$armadillo

  expected_cols <- c(
    "mpg", "cyl", "disp", "hp", "drat", "wt", "qsec", "vs", "am",
    "gear", "carb", "mpg_trans", "new_var"
  )

  expect_identical(res, expected_cols)
})

test_that("ds.rename renames variables", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.rename(
    df.name = "mtcars",
    tidy_expr = list(test_1 = mpg, test_2 = drat),
    newobj = "mpg_drat",
    datasources = conns
  )

  res <- dsBaseClient::ds.colnames("mpg_drat", datasources = conns)$armadillo

  expected_cols <- c(
    "test_1", "cyl", "disp", "hp", "test_2", "wt", "qsec", "vs", "am",
    "gear", "carb"
  )

  expect_identical(res, expected_cols)
})

test_that("ds.select selects variables", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.select(
    df.name = "mtcars",
    tidy_expr = list(mpg:drat),
    newobj = "mpg_drat_select",
    datasources = conns
  )

  res <- dsBaseClient::ds.colnames("mpg_drat_select", datasources = conns)$armadillo

  expected_cols <- c("mpg", "cyl", "disp", "hp", "drat")

  expect_identical(res, expected_cols)
})

test_that("ds.slice slices rows", {
  assign_tidyverse_data()

  conns <- test_env$conns

  ds.slice(
    df.name = "mtcars",
    tidy_expr = list(1:5),
    newobj = "sliced",
    datasources = conns
  )

  res <- dsBaseClient::ds.dim("sliced", datasources = conns)[[1]]

  expect_identical(res, as.integer(c(5, 11)))
})
