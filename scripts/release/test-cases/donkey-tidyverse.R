library(dsTidyverseClient)

verify_arrange <- function() {
  ds_function_name <- "ds.arrange"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.arrange(
    df.name = "mtcars",
    tidy_expr = list(cyl),
    newobj = "ordered_df",
    datasources = conns
    )
  
  res <- ds.class("ordered_df", datasources = conns)[[1]]
  verify_output(
    function_name = ds_function_name, object = res,
    expected = "data.frame",
    fail_msg = xenon_fail_msg$srv_class
  )
  
}

verify_as_tibble <- function() {
  ds_function_name <- "ds.as_tibble"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.as_tibble(
    x = "mtcars",
    newobj = "mtcars_tib",
    datasources = conns
  )
  
  res <- ds.class("mtcars_tib", datasources = conns)[[1]]

  verify_output(
    function_name = ds_function_name, object = res,
    expected = c("tbl_df", "tbl", "data.frame"),
    fail_msg = xenon_fail_msg$srv_class
  )
}

verify_bind_cols <- function() {
  ds_function_name <- "ds.bind_cols"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.bind_cols(
    to_combine = list(mtcars, mtcars),
    newobj = "cols_bound",
    datasources = conns
  )
  
  res <- ds.dim("cols_bound", datasources = conns)[[1]]
  verify_output(
    function_name = ds_function_name, object = res,
    expected = as.integer(c(32, 22)),
    fail_msg = xenon_fail_msg$srv_dim
  )
}

verify_bind_rows <- function() {
  ds_function_name <- "ds.bind_rows"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.bind_rows(
    to_combine = list(mtcars, mtcars),
    newobj = "rows_bound",
    datasources = conns
  )
  
  res <- ds.dim("rows_bound", datasources = conns)[[1]]
  
  verify_output(
    function_name = ds_function_name, object = res,
    expected = as.integer(c(64, 11)),
    fail_msg = xenon_fail_msg$srv_dim
  )
}

verify_case_when <- function() {
  ds_function_name <- "ds.case_when"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.case_when(
    tidy_expr = list(
      mtcars$mpg < 20 ~ "low",
      mtcars$mpg >= 20 & mtcars$mpg < 30 ~ "medium",
      mtcars$mpg >= 30 ~ "high"
    ),
    newobj = "test",
    datasources = conns
  )
  
  res <- names(ds.table("test", datasources = conns)$output.list$TABLES.COMBINED_all.sources_counts)

  verify_output(
    function_name = ds_function_name, object = res,
    expected = c("high", "low", "medium", "NA"),
    fail_msg = xenon_fail_msg$srv_lvl
  )
}

verify_distinct <- function() {
  ds_function_name <- "ds.distinct"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.distinct(
    df.name = "mtcars",
    tidy_expr = list(cyl, carb),
    newobj = "dist_df",
    datasources = conns
  )
  
  res <- ds.dim("dist_df", datasources = conns)[[1]]
  
  verify_output(
    function_name = ds_function_name, object = res,
    expected = as.integer(c(9, 2)),
    fail_msg = xenon_fail_msg$srv_dim
  )
}

verify_filter <- function() {
  ds_function_name <- "ds.filter"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.filter(
    df.name = "mtcars",
    tidy_expr = list(cyl == 4 & mpg > 20),
    newobj = "filtered",
    datasources = conns
  )
  
  res <- ds.dim("filtered", datasources = conns)[[1]]
    
  verify_output(
    function_name = ds_function_name, object = res,
    expected = as.integer(c(11, 11)),
    fail_msg = xenon_fail_msg$srv_dim
  )
}

verify_group_by <- function() {
  ds_function_name <- "ds.group_by"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.group_by(
    df.name = "mtcars",
    tidy_expr = list(mpg, cyl),
    newobj = "grouped",
    datasources = conns
  )
  
  res <- ds.class("grouped", datasources = conns)[[1]]
  
  verify_output(
    function_name = ds_function_name, object = res,
    expected = c("grouped_df", "tbl_df", "tbl", "data.frame"),
    fail_msg = xenon_fail_msg$srv_class
  )
}

verify_ungroup <- function() {
  ds_function_name <- "ds.ungroup"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.ungroup("mtcars_group", "ungrouped_df", datasources = conns)
  
  res <- ds.class("ungrouped_df", datasources = conns)[[1]]

  verify_output(
    function_name = ds_function_name, object = res,
    expected = c("tbl_df", "tbl", "data.frame"),
    fail_msg = xenon_fail_msg$srv_class
  )
}

# group_by
# group_keys
# if_else
# mutate
# rename
# select
# slice


run_tidyverse_tests <- function(skip_tests) {
  test_name <- "donkey-tidyverse"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }
  verify_arrange()
  verify_as_tibble()
  verify_bind_cols()
  verify_bind_rows()
  verify_case_when()
  verify_distinct()
  verify_filter()
  verify_group_by()
  verify_ungroup()
  cli_alert_success(sprintf("%s passed!", test_name))
}
