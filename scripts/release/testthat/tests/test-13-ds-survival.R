library(dsSurvivalClient)

# Setup
test_name <- "dsSurvival"
data_path <- "/survival/veteran"

# Assign survival data (setup, not a test)
if (!should_skip_test(test_name) && test_name %in% release_env$installed_ds_packages) {
  invisible(datashield.assign.table(release_env$conns, "survival", sprintf("%s%s", release_env$project1, data_path)))
}

test_that("ds.Surv creates correct class", {
  skip_ds_test(test_name)
  dsSurvivalClient::ds.Surv(
    time = "survival$time",
    event = "survival$status",
    objectname = "surv_object",
    datasources = release_env$conns
  )
  surv_class <- ds.class("surv_object", datasources = release_env$conns)
  expect_equal(surv_class$armadillo, "Surv")
})

test_that("ds.coxph.SLMA returns expected elements", {
  skip_ds_test(test_name)
  sink(nullfile())
  cox_output <- dsSurvivalClient::ds.coxph.SLMA(formula = "surv_object~survival$age",
                                                 datasources = release_env$conns)
  sink()
  expected_names <- c(
    "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients", "conf.int", "logtest",
    "sctest", "rsq", "waldtest", "used.robust", "concordance"
  )
  expect_identical(names(cox_output$armadillo), expected_names)
})

test_that("ds.coxphSLMAassign creates correct class", {
  skip_ds_test(test_name)
  sink(nullfile())
  dsSurvivalClient::ds.coxphSLMAassign(
    formula = "surv_object~survival$age",
    objectname = "coxph_serverside",
    datasources = release_env$conns
  )
  sink()
  cox_class <- ds.class("coxph_serverside", datasources = release_env$conns)
  expect_equal(cox_class$armadillo, "coxph")
})

test_that("ds.cox.zphSLMA returns expected elements", {
  skip_ds_test(test_name)
  hazard_assumption <- dsSurvivalClient::ds.cox.zphSLMA(fit = "coxph_serverside",
                                                         datasources = release_env$conns)
  expected_names <- c("table", "var", "transform", "call")
  expect_identical(names(hazard_assumption$armadillo), expected_names)
})

test_that("ds.coxphSummary returns expected elements", {
  skip_ds_test(test_name)
  hazard_summary <- dsSurvivalClient::ds.coxphSummary(x = "coxph_serverside",
                                                       datasources = release_env$conns)
  expected_names <- c(
    "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients",
    "conf.int", "logtest", "sctest", "rsq", "waldtest", "used.robust", "concordance"
  )
  expect_identical(names(hazard_summary$armadillo), expected_names)
})
