library(dsMTLClient)

# Setup
test_name <- "dsMTLBase"

prepare_data_for_lasso <- function() {
  ds.dataFrameSubset(
    V1 = "nonrep$row_id",
    V2 = "nonrep$row_id",
    Boolean.operator = "==",
    df.name = "nonrep",
    keep.cols = c(5, 9),
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

test_that("ds.LassoCov_Train returns expected names", {
  do_skip_test(test_name)
  skip_if_no_package(test_name)
  prepare_data_for_lasso()
  lasso_results <- ds.LassoCov_Train(
    X = "x_mat",
    Y = "y_mat",
    type = "regress",
    lambda = 500,
    covar = 1,
    nDigits = 2,
    datasources = release_env$conns
  )
  expect_identical(names(lasso_results), c("ws", "Logs", "Obj", "gamma", "type", "lam_seq"))
})
