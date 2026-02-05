# Setup
test_name <- "ds-base"
object <- "nonrep"
variable <- "coh_country"

test_that("ds.mean returns expected values", {
  do_skip_test(test_name)
  ds_mean <- ds.mean(paste0(object, "$", variable), datasources = release_env$conns)$Mean
  expect_equal(round(ds_mean[1], 3), 431.105)
  expect_equal(ds_mean[2], 0)
  expect_equal(ds_mean[3], 1000)
  expect_equal(ds_mean[4], 1000)
})

test_that("ds.histogram returns expected values", {
  do_skip_test(test_name)
  hist <- ds.histogram(x = paste0(object, "$", variable), datasources = release_env$conns)
  breaks <- c(35.31138, 116.38319, 197.45500, 278.52680, 359.59861, 440.67042, 521.74222, 602.81403, 683.88584, 764.95764, 846.02945)
  counts <- c(106, 101, 92, 103, 106, 104, 105, 101, 113, 69)
  density <- c(0.0013074829, 0.0012458092, 0.0011347965, 0.0012704787, 0.0013074829, 0.0012828134, 0.0012951481, 0.0012458092, 0.0013938261, 0.0008510974)
  mids <- c(75.84729, 156.91909, 237.99090, 319.06271, 400.13451, 481.20632, 562.27813, 643.34993, 724.42174, 805.49355)
  expect_equal(hist$breaks, breaks, tolerance = .Machine$double.eps^0.03)
  expect_equal(hist$counts, counts, tolerance = .Machine$double.eps^0.03)
  expect_equal(hist$density, density, tolerance = .Machine$double.eps^0.03)
  expect_equal(hist$mids, mids, tolerance = .Machine$double.eps^0.03)
})
