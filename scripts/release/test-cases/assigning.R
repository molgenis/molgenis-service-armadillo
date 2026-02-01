check_assigning <- function(folder, table, object, variable) {
  test_name <- "assigning"

  test_that("assign table returns data.frame", {
    do_skip_test(test_name)
    cli_alert_info(sprintf("Assigning table %s", table))
    datashield.assign.table(release_env$conns, table, sprintf("%s/%s/%s", release_env$project1, folder, table))
    datatype <- ds.class(x = table, datasources = release_env$conns)
    expected_type <- list()
    expected_type$armadillo <- "data.frame"
    expect_identical(datatype, expected_type)
  })

  test_that("assign expression succeeds", {
    do_skip_test(test_name)
    cli_alert_info(sprintf("Assigning expression for %s$%s", object, variable))
    expect_no_error(
      datashield.assign.expr(release_env$conns, "x", expr = as.symbol(paste0(object, "$", variable)))
    )
  })
}
