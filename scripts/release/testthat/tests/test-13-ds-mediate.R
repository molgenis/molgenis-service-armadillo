library(dsMediationClient)

# Setup
test_name <- "dsMediation"

# Assign mediate data (setup, not a test)
if (!should_skip_test(test_name) && test_name %in% release_env$installed_ds_packages) {
  datashield.assign.table(release_env$conns, "nonrep", sprintf("%s/2_1-core-1_0/nonrep", release_env$project1))
}

test_that("ds.mediate returns expected class", {
  do_skip_test(test_name)
  skip_if_no_package(test_name)
  ds.glmSLMA(
    formula = "agebirth_m_y ~ ethn3_m + sex", family = "gaussian", dataName = "nonrep",
    newobj = "med.fit.1a", datasources = release_env$conns
  )
  ds.glmSLMA(
    formula = "preg_dia ~ agebirth_m_y + ethn3_m + sex", family = "gaussian", dataName = "nonrep",
    newobj = "out.fit.1a", datasources = release_env$conns
  )
  ds.mediate(
    model.m = "med.fit.1a", model.y = "out.fit.1a", treat = "ethn3_m", mediator = "agebirth_m_y",
    boot = FALSE, conf.level = 0.95, robustSE = TRUE, sims = 100, seed = 123, newobj = "med.out.1a",
    datasources = release_env$conns
  )
  med_class <- ds.class("med.out.1a", datasources = release_env$conns)
  expect_equal(med_class$armadillo, "mediate")
})

test_that("ds.neWeight returns expected class", {
  do_skip_test(test_name)
  skip_if_no_package(test_name)
  ds.glmSLMA(
    formula = "agebirth_m_y ~ ethn3_m + sex", family = "gaussian", dataName = "nonrep",
    newobj = "med.fit.1b", datasources = release_env$conns
  )
  ds.neWeight(object = "med.fit.1b", newobj = "expData", datasources = release_env$conns)
  med_class <- ds.class("expData", datasources = release_env$conns)
  expect_identical(med_class$armadillo, c("data.frame", "expData", "weightData"))
})

test_that("ds.neModel returns expected class", {
  do_skip_test(test_name)
  skip_if_no_package(test_name)
  ds.neModel(
    formula = "preg_dia ~ ethn3_m0 + ethn3_m1 + sex",
    family = "gaussian", se = "robust", expData = "expData",
    newobj = "med.out.1b", datasources = release_env$conns
  )
  med_class <- ds.class("med.out.1b", datasources = release_env$conns)
  expect_equal(med_class$armadillo, "neModel")
})

test_that("ds.neImpute returns expected class", {
  do_skip_test(test_name)
  skip_if_no_package(test_name)
  ds.glmSLMA(
    formula = "preg_dia ~ agebirth_m_y + ethn3_m + sex",
    family = "gaussian", dataName = "nonrep", newobj = "out.fit.1c",
    datasources = release_env$conns
  )
  ds.neImpute(object = "out.fit.1c", nMed = 1, newobj = "impData", datasources = release_env$conns)
  med_class <- ds.class("impData", datasources = release_env$conns)
  expect_identical(med_class$armadillo, c("data.frame", "expData", "impData"))
})

test_that("ds.neLht returns expected class", {
  do_skip_test(test_name)
  skip_if_no_package(test_name)
  lht.out.1b <- ds.neLht(model = "med.out.1b", linfct = c("ethn3_m0=0", "ethn3_m1=0", "ethn3_m0+ethn3_m1=0"),
                          datasources = release_env$conns)
  med_class <- class(lht.out.1b$armadillo)
  expect_equal(med_class, "summary.neLht")
})
