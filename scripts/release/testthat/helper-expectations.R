# helper-expectations.R - Custom testthat expectations for release tests
#
# These expectations are designed for DataSHIELD testing where results may
# have small amounts of noise due to privacy-preserving calculations.

# Default tolerance for DataSHIELD noise comparisons.
# Native expect_equal(tolerance = ...) handles both scalars and lists recursively.
DS_TOLERANCE <- .Machine$double.eps^0.03

# -----------------------------------------------------------------------------
# Mean verification helper
# -----------------------------------------------------------------------------

#' Expect DataSHIELD mean values are correct
#'
#' Specialized expectation for verifying ds.mean output.
#'
#' @param ds_mean Result from ds.mean()$Mean
#' @param expected_mean Expected mean value (will be rounded to 3 decimals)
#' @param expected_valid_and_total Expected Nvalid and Ntotal values
expect_ds_mean_values <- function(ds_mean, expected_mean, expected_valid_and_total) {
  testthat::expect_equal(
    round(ds_mean[1], 3),
    expected_mean,
    label = "EstimatedMean"
  )

  testthat::expect_equal(
    ds_mean[2],
    0,
    label = "Nmissing"
  )

  testthat::expect_equal(
    ds_mean[3],
    expected_valid_and_total,
    label = "Nvalid"
  )

  testthat::expect_equal(
    ds_mean[4],
    expected_valid_and_total,
    label = "Ntotal"
  )
}
