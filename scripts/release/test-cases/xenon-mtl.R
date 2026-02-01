library(dsMTLClient)

prepare_data_for_lasso <- function() {
  ds.dataFrameSubset(
    V1 = "nonrep$row_id",
    V2 = "nonrep$row_id",
    Boolean.operator = "==",
    df.name = "nonrep",
    keep.cols = c(5, 9, 13, 17),
    newobj = "x_df",
    datasources = release_env$conns
  )

  ds.asDataMatrix("x_df", "x_mat", datasources = release_env$conns)

  ds.dataFrameSubset(
    V1 = "nonrep$row_id",
    V2 = "nonrep$row_id",
    Boolean.operator = "==",
    df.name = "nonrep",
    keep.cols = c(21),
    newobj = "y_df",
    datasources = release_env$conns
  )

  ds.asDataMatrix("y_df", "y_mat", datasources = release_env$conns)
}

verify_lasso_cov_train_output <- function() {
  lasso_results <- ds.LassoCov_Train(
    X = "x_mat",
    Y = "y_mat",
    type = "regress",
    lambda = 298.9465,
    covar = 1,
    nDigits = 4,
    datasources = release_env$conns
  )

  if (identical(names(lasso_results), c("ws", "Logs", "Obj", "gamma", "type", "lam_seq"))) {
    cli_alert_success("ds.LassoCov_Train passed")
  } else {
    cli_alert_danger("ds.LassoCov_Train failed")
    exit_test("ds.LassoCov_Train did not return an object with expected names")
  }
}

verify_ds_mtl <- function() {
  test_name <- "xenon-mtl"
  if (do_skip_test(test_name)) {
    return()
  }
  prepare_data_for_lasso()
  verify_lasso_cov_train_output()
  cli_alert_success(sprintf("%s passed!", test_name))
}
