# test-30-ds-base.R - dsBase package tests
#
# These tests verify that dsBase functions work correctly.

# Setup: ensure researcher connection is established
ensure_researcher_login_and_assign()

# Helper to check all skip conditions for this test file
skip_if_ds_base_excluded <- function() {
  skip_if_excluded("ds-base")
}

test_that("ds.mean returns expected values", {
  skip_if_ds_base_excluded()
  ds_mean <- dsBaseClient::ds.mean(
    "nonrep$coh_country",
    datasources = conns
  )$Mean

  expect_ds_mean_values(ds_mean, 431.105, 1000)
})

# Note: ds.histogram emits "0 invalid cells" warning which is expected behavior
# (indicates all cells passed disclosure checks). We suppress it to keep test output clean.

test_that("ds.histogram returns expected breaks", {
  skip_if_ds_base_excluded()
  hist <- suppressWarnings(dsBaseClient::ds.histogram(
    x = "nonrep$coh_country",
    datasources = conns
  ))

  expected_breaks <- c(
    35.31138, 116.38319, 197.45500, 278.52680, 359.59861,
    440.67042, 521.74222, 602.81403, 683.88584, 764.95764, 846.02945
  )

  expect_equal(hist$breaks, expected_breaks, tolerance = DS_TOLERANCE)
})

test_that("ds.histogram returns expected counts", {
  skip_if_ds_base_excluded()
  hist <- suppressWarnings(dsBaseClient::ds.histogram(
    x = "nonrep$coh_country",
    datasources = conns
  ))

  expected_counts <- c(106, 101, 92, 103, 106, 104, 105, 101, 113, 69)

  expect_equal(hist$counts, expected_counts, tolerance = DS_TOLERANCE)
})

test_that("ds.histogram returns expected density", {
  skip_if_ds_base_excluded()
  hist <- suppressWarnings(dsBaseClient::ds.histogram(
    x = "nonrep$coh_country",
    datasources = conns
  ))

  expected_density <- c(
    0.0013074829, 0.0012458092, 0.0011347965, 0.0012704787, 0.0013074829,
    0.0012828134, 0.0012951481, 0.0012458092, 0.0013938261, 0.0008510974
  )

  expect_equal(hist$density, expected_density, tolerance = DS_TOLERANCE)
})

test_that("ds.histogram returns expected mids", {
  skip_if_ds_base_excluded()
  hist <- suppressWarnings(dsBaseClient::ds.histogram(
    x = "nonrep$coh_country",
    datasources = conns
  ))

  expected_mids <- c(
    75.84729, 156.91909, 237.99090, 319.06271, 400.13451,
    481.20632, 562.27813, 643.34993, 724.42174, 805.49355
  )

  expect_equal(hist$mids, expected_mids, tolerance = DS_TOLERANCE)
})
