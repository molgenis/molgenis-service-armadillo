check_tables_assign <- function(folder, table) {
  cli_alert_info(sprintf("Assigning table %s", table))
  datashield.assign.table(release_env$conns, table, sprintf("%s/%s/%s", release_env$project1, folder, table))
  datatype <- ds.class(x = table, datasources = release_env$conns)
  expected_type <- list()
  expected_type$armadillo <- "data.frame"

  if (identical(datatype, expected_type)) {
    cli_alert_success("Assigned table is dataframe")
  } else {
    cli_alert_danger("Assigned table not of expected type:")
    print(datatype)
  }
}

check_expression_assign <- function(object, variable) {
  cli_alert_info(sprintf("Assigning expression for %s$%s", object, variable))
  datashield.assign.expr(release_env$conns, "x", expr = as.symbol(paste0(object, "$", variable)))
  cli_alert_success("Expression assigned")
}

check_assigning <- function() {
  test_name <- "assigning"
  if (do_skip_test(test_name)) {
    return()
  }
  check_tables_assign("2_1-core-1_0", "nonrep")
  check_expression_assign("nonrep", "coh_country")
  cli_alert_success(sprintf("%s passed!", test_name))
}
