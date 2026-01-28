# test-30-ds-base.R - dsBase package tests
#
# These tests verify that dsBase functions work correctly.

# Setup: ensure researcher connection is established
ensure_researcher_login()

# Skip all tests if ds-base is excluded
skip_if_excluded("ds-base")

test_that("ds.mean returns expected values", {
  conns <- test_env$conns

  ds_mean <- dsBaseClient::ds.mean(
    "nonrep$coh_country",
    datasources = conns
  )$Mean

  # Verify mean values
  # EstimatedMean should be ~431.105

  expect_equal(round(ds_mean[1], 3), 431.105, label = "EstimatedMean")

  # Nmissing should be 0
  expect_equal(ds_mean[2], 0, label = "Nmissing")

  # Nvalid should be 1000
  expect_equal(ds_mean[3], 1000, label = "Nvalid")

  # Ntotal should be 1000
  expect_equal(ds_mean[4], 1000, label = "Ntotal")
})

test_that("ds.histogram returns expected breaks", {
  conns <- test_env$conns

  hist <- dsBaseClient::ds.histogram(
    x = "nonrep$coh_country",
    datasources = conns
  )

  expected_breaks <- c(
    35.31138, 116.38319, 197.45500, 278.52680, 359.59861,
    440.67042, 521.74222, 602.81403, 683.88584, 764.95764, 846.02945
  )

  expect_almost_equal_list(hist$breaks, expected_breaks)
})

test_that("ds.histogram returns expected counts", {
  conns <- test_env$conns

  hist <- dsBaseClient::ds.histogram(
    x = "nonrep$coh_country",
    datasources = conns
  )

  expected_counts <- c(106, 101, 92, 103, 106, 104, 105, 101, 113, 69)

  expect_almost_equal_list(hist$counts, expected_counts)
})

test_that("ds.histogram returns expected density", {
  conns <- test_env$conns

  hist <- dsBaseClient::ds.histogram(
    x = "nonrep$coh_country",
    datasources = conns
  )

  expected_density <- c(
    0.0013074829, 0.0012458092, 0.0011347965, 0.0012704787, 0.0013074829,
    0.0012828134, 0.0012951481, 0.0012458092, 0.0013938261, 0.0008510974
  )

  expect_almost_equal_list(hist$density, expected_density)
})

test_that("ds.histogram returns expected mids", {
  conns <- test_env$conns

  hist <- dsBaseClient::ds.histogram(
    x = "nonrep$coh_country",
    datasources = conns
  )

  expected_mids <- c(
    75.84729, 156.91909, 237.99090, 319.06271, 400.13451,
    481.20632, 562.27813, 643.34993, 724.42174, 805.49355
  )

  expect_almost_equal_list(hist$mids, expected_mids)
})
