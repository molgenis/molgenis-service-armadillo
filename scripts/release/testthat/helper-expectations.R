# helper-expectations.R - Custom testthat expectations for release tests
#
# These expectations are designed for DataSHIELD testing where results may
# have small amounts of noise due to privacy-preserving calculations.

# -----------------------------------------------------------------------------
# Tolerance-based expectations
# -----------------------------------------------------------------------------

#' Expect values are almost equal (with tolerance for DataSHIELD noise)
#'
#' DataSHIELD adds small amounts of noise to protect privacy. This expectation
#' accounts for that by using a configurable tolerance.
#'
#' @param object The actual value
#' @param expected The expected value
#' @param tolerance Tolerance for comparison (default matches almost_equal from common-functions.R)
#' @param label Optional label for the object
#' @export
expect_almost_equal <- function(object, expected,
                                 tolerance = .Machine$double.eps^0.03,
                                 label = NULL) {
  if (is.null(label)) {
    label <- deparse(substitute(object))
  }

  testthat::expect_equal(object, expected, tolerance = tolerance, label = label)
}

#' Expect list values are almost equal element-wise
#'
#' Compares each element of two lists/vectors with tolerance.
#'
#' @param object The actual list/vector
#' @param expected The expected list/vector
#' @param tolerance Tolerance for comparison
#' @param label Optional label
#' @export
expect_almost_equal_list <- function(object, expected,
                                      tolerance = .Machine$double.eps^0.03,
                                      label = NULL) {
  if (is.null(label)) {
    label <- deparse(substitute(object))
  }

  testthat::expect_length(object, length(expected))

  for (i in seq_along(expected)) {
    expect_almost_equal(
      object[[i]],
      expected[[i]],
      tolerance = tolerance,
      label = sprintf("%s[[%d]]", label, i)
    )
  }
}

# -----------------------------------------------------------------------------
# DataSHIELD-specific expectations
# -----------------------------------------------------------------------------

#' Expect DataSHIELD output matches expected value
#'
#' Direct port of verify_output from common-functions.R.
#' Checks for identical values.
#'
#' @param object The actual value
#' @param expected The expected value
#' @param label Optional label for error messages
#' @param fail_msg Optional failure message suffix
#' @export
expect_ds_output <- function(object, expected, label = NULL, fail_msg = NULL) {
  if (is.null(label)) {
    label <- deparse(substitute(object))
  }

  testthat::expect_identical(
    object,
    expected,
    label = label,
    info = fail_msg
  )
}

#' Expect DataSHIELD class matches expected
#'
#' Checks that a serverside or clientside object has the expected class.
#'
#' @param object Result from ds.class()
#' @param expected Expected class (string or character vector)
#' @param server Server name in connections (default "armadillo")
#' @param label Optional label
#' @export
expect_ds_class <- function(object, expected, server = "armadillo", label = NULL) {
  if (is.null(label)) {
    label <- deparse(substitute(object))
  }

  actual <- if (is.list(object)) object[[server]] else object

  testthat::expect_identical(
    as.character(actual),
    as.character(expected),
    label = label
  )
}

#' Expect DataSHIELD dimensions match expected
#'
#' Checks that a serverside object has the expected dimensions.
#'
#' @param object Result from ds.dim()
#' @param expected Expected dimensions as integer vector c(rows, cols)
#' @param server Server name in connections (default "armadillo")
#' @param label Optional label
#' @export
expect_ds_dim <- function(object, expected, server = "armadillo", label = NULL) {
  if (is.null(label)) {
    label <- deparse(substitute(object))
  }

  actual <- if (is.list(object)) object[[server]] else object

  testthat::expect_identical(
    as.integer(actual),
    as.integer(expected),
    label = label
  )
}

#' Expect DataSHIELD column names match expected
#'
#' Checks that a serverside object has the expected column names.
#'
#' @param object Result from ds.colnames()
#' @param expected Expected column names
#' @param server Server name in connections (default "armadillo")
#' @param label Optional label
#' @export
expect_ds_colnames <- function(object, expected, server = "armadillo", label = NULL) {
  if (is.null(label)) {
    label <- deparse(substitute(object))
  }

  actual <- if (is.list(object)) object[[server]] else object

  testthat::expect_identical(actual, expected, label = label)
}

#' Expect DataSHIELD list has expected names
#'
#' Checks that a clientside list has the expected element names.
#'
#' @param object The clientside list
#' @param expected Expected names
#' @param server Server name in connections (default "armadillo")
#' @param label Optional label
#' @export
expect_ds_names <- function(object, expected, server = "armadillo", label = NULL) {
  if (is.null(label)) {
    label <- deparse(substitute(object))
  }

  actual <- if (is.list(object) && !is.null(object[[server]])) {
    names(object[[server]])
  } else {
    names(object)
  }

  testthat::expect_identical(actual, expected, label = label)
}

# -----------------------------------------------------------------------------
# Comparison helpers
# -----------------------------------------------------------------------------

#' Check if two values are almost equal (non-asserting)
#'
#' Returns TRUE/FALSE instead of throwing an error. Useful for conditional logic.
#'
#' @param val1 First value
#' @param val2 Second value
#' @param tolerance Tolerance for comparison
#' @return TRUE if values are equal within tolerance
#' @export
almost_equal <- function(val1, val2, tolerance = .Machine$double.eps^0.03) {
  result <- all.equal(val1, val2, tolerance = tolerance)
  return(isTRUE(result))
}

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
#' @export
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
