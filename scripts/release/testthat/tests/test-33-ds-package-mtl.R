# test-33-ds-package-mtl.R - dsMTL package tests
#
# These tests verify that dsMTL functions work correctly.

# Setup: ensure researcher connection is established
ensure_researcher_login()

# Skip all tests if ds-package-mtl is excluded
skip_if_excluded("ds-package-mtl")

# Load the MTL client library
library(dsMTLClient)

test_that("data can be prepared for lasso", {
  # Create subset for X matrix
  dsBaseClient::ds.dataFrameSubset(
    V1 = "nonrep$row_id",
    V2 = "nonrep$row_id",
    Boolean.operator = "==",
    df.name = "nonrep",
    keep.cols = c(5, 9, 13, 17),
    newobj = "x_df",
    datasources = conns
  )

  dsBaseClient::ds.asDataMatrix("x_df", "x_mat", datasources = conns())

  # Create subset for Y matrix
  dsBaseClient::ds.dataFrameSubset(
    V1 = "nonrep$row_id",
    V2 = "nonrep$row_id",
    Boolean.operator = "==",
    df.name = "nonrep",
    keep.cols = c(21),
    newobj = "y_df",
    datasources = conns
  )

  dsBaseClient::ds.asDataMatrix("y_df", "y_mat", datasources = conns())

  # Verify matrices were created
  x_class <- dsBaseClient::ds.class("x_mat", datasources = conns())
  y_class <- dsBaseClient::ds.class("y_mat", datasources = conns())

  expect_true("matrix" %in% x_class$armadillo)
  expect_true("matrix" %in% y_class$armadillo)
})

test_that("ds.LassoCov_Train returns expected output", {
  # Ensure data is prepared
  tryCatch({
    dsBaseClient::ds.class("x_mat", datasources = conns())
  }, error = function(e) {
    dsBaseClient::ds.dataFrameSubset(
      V1 = "nonrep$row_id",
      V2 = "nonrep$row_id",
      Boolean.operator = "==",
      df.name = "nonrep",
      keep.cols = c(5, 9, 13, 17),
      newobj = "x_df",
      datasources = conns
    )
    dsBaseClient::ds.asDataMatrix("x_df", "x_mat", datasources = conns())
    dsBaseClient::ds.dataFrameSubset(
      V1 = "nonrep$row_id",
      V2 = "nonrep$row_id",
      Boolean.operator = "==",
      df.name = "nonrep",
      keep.cols = c(21),
      newobj = "y_df",
      datasources = conns
    )
    dsBaseClient::ds.asDataMatrix("y_df", "y_mat", datasources = conns())
  })

  # Run Lasso with covariance
  lasso_results <- ds.LassoCov_Train(
    X = "x_mat",
    Y = "y_mat",
    type = "regress",
    lambda = 298.9465,
    covar = 1,
    nDigits = 4,
    datasources = conns
  )

  expected_names <- c("ws", "Logs", "Obj", "gamma", "type", "lam_seq")

  expect_identical(names(lasso_results), expected_names)
})
