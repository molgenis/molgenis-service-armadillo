# test-31-ds-package-mediate.R - dsMediation package tests
#
# These tests verify that dsMediation functions work correctly.

# Setup: ensure researcher connection is established
ensure_researcher_login()

# Skip all tests if ds-package-mediate is excluded
skip_if_excluded("ds-package-mediate")

# Load the mediation client library
library(dsMediationClient)

test_that("ds.mediate creates object with expected class", {
  # Fit mediator model
  dsBaseClient::ds.glmSLMA(
    formula = "agebirth_m_y ~ ethn3_m + sex",
    family = "gaussian",
    dataName = "nonrep",
    newobj = "med.fit.1a",
    datasources = conns
  )

  # Fit outcome model
  dsBaseClient::ds.glmSLMA(
    formula = "preg_dia ~ agebirth_m_y + ethn3_m + sex",
    family = "gaussian",
    dataName = "nonrep",
    newobj = "out.fit.1a",
    datasources = conns
  )

  # Run mediation analysis
  med_out <- ds.mediate(
    model.m = "med.fit.1a",
    model.y = "out.fit.1a",
    treat = "ethn3_m",
    mediator = "agebirth_m_y",
    boot = FALSE,
    conf.level = 0.95,
    robustSE = TRUE,
    sims = 100,
    seed = 123,
    newobj = "med.out.1a",
    datasources = conns
  )

  # Check class
  med_class <- dsBaseClient::ds.class("med.out.1a", datasources = conns())

  expect_equal(as.character(med_class), "mediate")
})

test_that("ds.neWeight creates object with expected class", {
  # Fit model
  dsBaseClient::ds.glmSLMA(
    formula = "agebirth_m_y ~ ethn3_m + sex",
    family = "gaussian",
    dataName = "nonrep",
    newobj = "med.fit.1b",
    datasources = conns
  )

  # Create weight data
  ds.neWeight(object = "med.fit.1b", newobj = "expData", datasources = conns())

  # Check class
  med_class <- dsBaseClient::ds.class("expData", datasources = conns())

  expect_identical(
    med_class$armadillo,
    c("data.frame", "expData", "weightData")
  )
})

test_that("ds.neModel creates object with expected class", {
  # This test depends on expData from previous test
  # If running in isolation, we need to set up expData first
  tryCatch({
    dsBaseClient::ds.class("expData", datasources = conns())
  }, error = function(e) {
    dsBaseClient::ds.glmSLMA(
      formula = "agebirth_m_y ~ ethn3_m + sex",
      family = "gaussian",
      dataName = "nonrep",
      newobj = "med.fit.1b",
      datasources = conns
    )
    ds.neWeight(object = "med.fit.1b", newobj = "expData", datasources = conns())
  })

  # Fit model
  med.out.1b <- ds.neModel(
    formula = "preg_dia ~ ethn3_m0 + ethn3_m1 + sex",
    family = "gaussian",
    se = "robust",
    expData = "expData",
    newobj = "med.out.1b",
    datasources = conns
  )

  # Check class
  med_class <- dsBaseClient::ds.class("med.out.1b", datasources = conns())

  expect_equal(as.character(med_class), "neModel")
})

test_that("ds.neImpute creates object with expected class", {
  # Fit outcome model
  out.fit.1c <- dsBaseClient::ds.glmSLMA(
    formula = "preg_dia ~ agebirth_m_y + ethn3_m + sex",
    family = "gaussian",
    dataName = "nonrep",
    newobj = "out.fit.1c",
    datasources = conns
  )

  # Create imputed data
  ds.neImpute(object = "out.fit.1c", nMed = 1, newobj = "impData", datasources = conns())

  # Check class
  med_class <- dsBaseClient::ds.class("impData", datasources = conns())

  expect_identical(
    med_class$armadillo,
    c("data.frame", "expData", "impData")
  )
})

test_that("ds.neLht returns object with expected class", {
  # Ensure med.out.1b exists
  tryCatch({
    dsBaseClient::ds.class("med.out.1b", datasources = conns())
  }, error = function(e) {
    # Set up if not exists
    dsBaseClient::ds.glmSLMA(
      formula = "agebirth_m_y ~ ethn3_m + sex",
      family = "gaussian",
      dataName = "nonrep",
      newobj = "med.fit.1b",
      datasources = conns
    )
    ds.neWeight(object = "med.fit.1b", newobj = "expData", datasources = conns())
    ds.neModel(
      formula = "preg_dia ~ ethn3_m0 + ethn3_m1 + sex",
      family = "gaussian",
      se = "robust",
      expData = "expData",
      newobj = "med.out.1b",
      datasources = conns
    )
  })

  # Run linear hypothesis test
  lht.out.1b <- ds.neLht(
    model = "med.out.1b",
    linfct = c("ethn3_m0=0", "ethn3_m1=0", "ethn3_m0+ethn3_m1=0"),
    datasources = conns
  )

  # Check class
  med_class <- class(lht.out.1b$armadillo)

  expect_equal(med_class, "summary.neLht")
})
