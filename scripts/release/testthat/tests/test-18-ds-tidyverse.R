library(dsTidyverseClient)

# Setup
test_name <- "ds-tidyverse"
data_path <- "/tidyverse"

# Assign tidyverse data (setup, not a test)
if (!test_name %in% release_env$skip_tests) {
  datashield.assign.table(release_env$conns, "mtcars", sprintf("%s%s/mtcars", release_env$project1, data_path))
  datashield.assign.table(release_env$conns, "mtcars_group", sprintf("%s%s/mtcars_group", release_env$project1, data_path))
}

test_that("ds.arrange", {
  do_skip_test(test_name)
  ds.arrange(
    df.name = "mtcars",
    tidy_expr = list(cyl),
    newobj = "ordered_df",
    datasources = release_env$conns
  )
  res <- ds.class("ordered_df", datasources = release_env$conns)[[1]]
  expect_identical(res, "data.frame")
})

test_that("ds.as_tibble", {
  do_skip_test(test_name)
  ds.as_tibble(
    x = "mtcars",
    newobj = "mtcars_tib",
    datasources = release_env$conns
  )
  res <- ds.class("mtcars_tib", datasources = release_env$conns)[[1]]
  expect_identical(res, c("tbl_df", "tbl", "data.frame"))
})

test_that("ds.bind_cols", {
  do_skip_test(test_name)
  ds.bind_cols(
    to_combine = list(mtcars, mtcars),
    newobj = "cols_bound",
    datasources = release_env$conns
  )
  res <- ds.dim("cols_bound", datasources = release_env$conns)[[1]]
  expect_identical(res, as.integer(c(32, 22)))
})

test_that("ds.bind_rows", {
  do_skip_test(test_name)
  ds.bind_rows(
    to_combine = list(mtcars, mtcars),
    newobj = "rows_bound",
    datasources = release_env$conns
  )
  res <- ds.dim("rows_bound", datasources = release_env$conns)[[1]]
  expect_identical(res, as.integer(c(64, 11)))
})

test_that("ds.case_when", {
  do_skip_test(test_name)
  ds.case_when(
    tidy_expr = list(
      mtcars$mpg < 20 ~ "low",
      mtcars$mpg >= 20 & mtcars$mpg < 30 ~ "medium",
      mtcars$mpg >= 30 ~ "high"
    ),
    newobj = "test",
    datasources = release_env$conns
  )
  # Suppress "Data in all studies were valid" output from ds.table
  invisible(capture.output(
    tbl_result <- ds.table("test", datasources = release_env$conns)
  ))
  res <- names(tbl_result$output.list$TABLES.COMBINED_all.sources_counts)
  expect_identical(res, c("high", "low", "medium", "NA"))
})

test_that("ds.distinct", {
  do_skip_test(test_name)
  ds.distinct(
    df.name = "mtcars",
    tidy_expr = list(cyl, carb),
    newobj = "dist_df",
    datasources = release_env$conns
  )
  res <- ds.dim("dist_df", datasources = release_env$conns)[[1]]
  expect_identical(res, as.integer(c(9, 2)))
})

test_that("ds.filter", {
  do_skip_test(test_name)
  ds.filter(
    df.name = "mtcars",
    tidy_expr = list(cyl == 4 & mpg > 20),
    newobj = "filtered",
    datasources = release_env$conns
  )
  res <- ds.dim("filtered", datasources = release_env$conns)[[1]]
  expect_identical(res, as.integer(c(11, 11)))
})

test_that("ds.group_by", {
  do_skip_test(test_name)
  ds.group_by(
    df.name = "mtcars",
    tidy_expr = list(cyl),
    newobj = "grouped",
    datasources = release_env$conns
  )
  res <- ds.class("grouped", datasources = release_env$conns)[[1]]
  expect_identical(res, c("grouped_df", "tbl_df", "tbl", "data.frame"))
})

test_that("ds.ungroup", {
  do_skip_test(test_name)
  ds.ungroup("grouped", "ungrouped_df", datasources = release_env$conns)
  res <- ds.class("ungrouped_df", datasources = release_env$conns)[[1]]
  expect_identical(res, c("tbl_df", "tbl", "data.frame"))
})

test_that("ds.group_keys", {
  do_skip_test(test_name)
  res <- ds.group_keys("grouped", datasources = release_env$conns)$armadillo
  expect_identical(res, tibble(cyl = c(4, 6, 8)))
})

test_that("ds.if_else", {
  do_skip_test(test_name)
  ds.if_else(
    condition = list(mtcars$mpg > 20),
    "high",
    "low",
    newobj = "test",
    datasources = release_env$conns
  )
  # Suppress "Data in all studies were valid" output from ds.table
  invisible(capture.output(
    tbl_result <- ds.table("test", datasources = release_env$conns)
  ))
  res <- names(tbl_result$output.list$TABLES.COMBINED_all.sources_counts)
  expect_identical(res, c("high", "low", "NA"))
})

test_that("ds.mutate", {
  do_skip_test(test_name)
  ds.mutate(
    df.name = "mtcars",
    tidy_expr = list(mpg_trans = cyl * 1000, new_var = (hp - drat) / qsec),
    newobj = "new",
    datasources = release_env$conns
  )
  res <- ds.colnames("new", datasources = release_env$conns)$armadillo
  expect_identical(res, c("mpg", "cyl", "disp", "hp", "drat", "wt", "qsec", "vs", "am", "gear", "carb", "mpg_trans", "new_var"))
})

test_that("ds.rename", {
  do_skip_test(test_name)
  ds.rename(
    df.name = "mtcars",
    tidy_expr = list(test_1 = mpg, test_2 = drat),
    newobj = "mpg_drat",
    datasources = release_env$conns
  )
  res <- ds.colnames("mpg_drat", datasources = release_env$conns)$armadillo
  expect_identical(res, c("test_1", "cyl", "disp", "hp", "test_2", "wt", "qsec", "vs", "am", "gear", "carb"))
})

test_that("ds.select", {
  do_skip_test(test_name)
  ds.select(
    df.name = "mtcars",
    tidy_expr = list(mpg:drat),
    newobj = "mpg_drat",
    datasources = release_env$conns
  )
  res <- ds.colnames("mpg_drat", datasources = release_env$conns)$armadillo
  expect_identical(res, c("mpg", "cyl", "disp", "hp", "drat"))
})

test_that("ds.slice", {
  do_skip_test(test_name)
  ds.slice(
    df.name = "mtcars",
    tidy_expr = list(1:5),
    newobj = "sliced",
    datasources = release_env$conns
  )
  res <- ds.dim("sliced", datasources = release_env$conns)[[1]]
  expect_identical(res, as.integer(c(5, 11)))
})
