library(dsTidyverseClient)

assign_tidyverse_data <- function(project, data_path) {
  cli_alert_info(sprintf("Assigning table: [%s%s/mtcars]", project, data_path))
  datashield.assign.table(conns, "mtcars", sprintf("%s%s/mtcars", project, data_path))
  cli_alert_info(sprintf("Assigning table: [%s%s/mtcars_group]", project, data_path))
  datashield.assign.table(conns, "mtcars_group", sprintf("%s%s/mtcars_group", project, data_path))
}

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
    tidy_expr = list(cyl),
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
  ds.ungroup("grouped", "ungrouped_df", datasources = conns)
  res <- ds.class("ungrouped_df", datasources = conns)[[1]]

  verify_output(
    function_name = ds_function_name, object = res,
    expected = c("tbl_df", "tbl", "data.frame"),
    fail_msg = xenon_fail_msg$srv_class
  )
}

verify_group_keys <- function() {
  ds_function_name <- "ds.group_keys"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  res <- ds.group_keys("grouped", datasources = conns)$armadillo
  
  verify_output(
    function_name = ds_function_name, object = res,
    expected = tibble(cyl = c(4, 6, 8)),
    fail_msg = xenon_fail_msg$clt_grp
  )
}

verify_if_else <- function() {
  ds_function_name <- "ds.if_else"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  
  ds.if_else(
    condition = list(mtcars$mpg > 20),
    "high",
    "low",
    newobj = "test",
    datasources = conns
  )
  
  res <- names(ds.table("test", datasources = conns)$output.list$TABLES.COMBINED_all.sources_counts)
  
  verify_output(
    function_name = ds_function_name, object = res,
    expected = c("high", "low", "NA"),
    fail_msg = xenon_fail_msg$srv_lvl
  )
}

verify_mutate <- function() {
  ds_function_name <- "ds.mutate"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  
  ds.mutate(
    df.name = "mtcars",
    tidy_expr = list(mpg_trans = cyl * 1000, new_var = (hp - drat) / qsec),
    newobj = "new",
    datasources = conns
  )
  
  res <- ds.colnames("new")$armadillo
  
  verify_output(
    function_name = ds_function_name, object = res,
    expected = c("mpg", "cyl", "disp", "hp", "drat", "wt", "qsec", "vs", "am", "gear", "carb", "mpg_trans", "new_var"),
    fail_msg = xenon_fail_msg$srv_var
  )
}

verify_rename <- function() {
  ds_function_name <- "ds.rename"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  
  ds.rename(
    df.name = "mtcars",
    tidy_expr = list(test_1 = mpg, test_2 = drat),
    newobj = "mpg_drat",
    datasources = conns
  )
  res <- ds.colnames("mpg_drat", datasources = conns)$armadillo
  
  verify_output(
    function_name = ds_function_name, object = res,
    expected = c("test_1", "cyl", "disp", "hp", "test_2", "wt", "qsec", "vs", "am", "gear", "carb"),
    fail_msg = xenon_fail_msg$srv_var
  )
}

verify_select <- function() {
  ds_function_name <- "ds.select"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  
  ds.select(
    df.name = "mtcars",
    tidy_expr = list(mpg:drat),
    newobj = "mpg_drat",
    datasources = conns
  )
  res <-  ds.colnames("mpg_drat", datasources = conns)$armadillo
  
  verify_output(
    function_name = ds_function_name, object = res,
    expected = c("mpg", "cyl", "disp", "hp", "drat"),
    fail_msg = xenon_fail_msg$srv_var
  )
}

verify_slice <- function() {
  ds_function_name <- "ds.slice"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  
  ds.slice(
    df.name = "mtcars",
    tidy_expr = list(1:5),
    newobj = "sliced",
    datasources = conns
  )
  
  res <-  ds.dim("sliced", datasources = conns)[[1]]
  
  verify_output(
    function_name = ds_function_name, object = res,
    expected = as.integer(c(5, 11)),
    fail_msg = xenon_fail_msg$srv_dim)
}

run_tidyverse_tests <- function(skip_tests, project, data_path) {
  test_name <- "donkey-tidyverse"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }
  assign_tidyverse_data(project, data_path)
  verify_arrange()
  verify_as_tibble()
  verify_bind_cols()
  verify_bind_rows()
  verify_case_when()
  verify_distinct()
  verify_filter()
  verify_group_by()
  verify_ungroup()
  verify_group_keys()
  verify_if_else()
  verify_mutate()
  verify_rename()
  verify_select()
  verify_slice()
  cli_alert_success(sprintf("%s passed!", test_name))
}
