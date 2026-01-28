# test-32-ds-package-survival.R - dsSurvival package tests
#
# These tests verify that dsSurvival functions work correctly.

# Setup: ensure researcher connection is established
ensure_researcher_login()

# Skip all tests if ds-package-survival is excluded
skip_if_excluded("ds-package-survival")

# Load the survival client library
library(dsSurvivalClient)

# Helper to assign survival data
assign_survival_data <- function() {
  data_path <- "/survival/veteran"

  DSI::datashield.assign.table(
    conns(),
    "survival",
    sprintf("%s%s", project(), data_path)
  )
}

test_that("survival data can be assigned", {
  assign_survival_data()

  datatype <- dsBaseClient::ds.class(x = "survival", datasources = conns())

  expect_equal(datatype$armadillo, "data.frame")
})

test_that("ds.Surv creates object with expected class", {
  # Ensure data is assigned
  assign_survival_data()


  # Create survival object
  dsSurvivalClient::ds.Surv(
    time = "survival$time",
    event = "survival$status",
    objectname = "surv_object",
    datasources = conns
  )

  # Check class
  surv_class <- dsBaseClient::ds.class("surv_object", datasources = conns())

  expect_equal(surv_class$armadillo, "Surv")
})

test_that("ds.coxph.SLMA returns expected elements", {

  # Ensure survival object exists
  tryCatch({
    dsBaseClient::ds.class("surv_object", datasources = conns())
  }, error = function(e) {
    assign_survival_data()
    dsSurvivalClient::ds.Surv(
      time = "survival$time",
      event = "survival$status",
      objectname = "surv_object",
      datasources = conns
    )
  })

  # Run Cox regression
  cox_output <- dsSurvivalClient::ds.coxph.SLMA(
    formula = "surv_object~survival$age",
    datasources = conns
  )

  expected_names <- c(
    "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients",
    "conf.int", "logtest", "sctest", "rsq", "waldtest", "used.robust",
    "concordance"
  )

  expect_identical(names(cox_output$armadillo), expected_names)
})

test_that("ds.coxphSLMAassign creates object with expected class", {

  # Ensure survival object exists
  tryCatch({
    dsBaseClient::ds.class("surv_object", datasources = conns())
  }, error = function(e) {
    assign_survival_data()
    dsSurvivalClient::ds.Surv(
      time = "survival$time",
      event = "survival$status",
      objectname = "surv_object",
      datasources = conns
    )
  })

  # Assign Cox model object
  dsSurvivalClient::ds.coxphSLMAassign(
    formula = "surv_object~survival$age",
    objectname = "coxph_serverside",
    datasources = conns
  )

  # Check class
  cox_class <- dsBaseClient::ds.class("coxph_serverside", datasources = conns())

  expect_equal(cox_class$armadillo, "coxph")
})

test_that("ds.cox.zphSLMA returns expected elements", {

  # Ensure coxph object exists
  tryCatch({
    dsBaseClient::ds.class("coxph_serverside", datasources = conns())
  }, error = function(e) {
    assign_survival_data()
    dsSurvivalClient::ds.Surv(
      time = "survival$time",
      event = "survival$status",
      objectname = "surv_object",
      datasources = conns
    )
    dsSurvivalClient::ds.coxphSLMAassign(
      formula = "surv_object~survival$age",
      objectname = "coxph_serverside",
      datasources = conns
    )
  })

  # Test proportional hazards assumption
  hazard_assumption <- dsSurvivalClient::ds.cox.zphSLMA(
    fit = "coxph_serverside",
    datasources = conns
  )

  expected_names <- c("table", "var", "transform", "call")

  expect_identical(names(hazard_assumption$armadillo), expected_names)
})

test_that("ds.coxphSummary returns expected elements", {

  # Ensure coxph object exists
  tryCatch({
    dsBaseClient::ds.class("coxph_serverside", datasources = conns())
  }, error = function(e) {
    assign_survival_data()
    dsSurvivalClient::ds.Surv(
      time = "survival$time",
      event = "survival$status",
      objectname = "surv_object",
      datasources = conns
    )
    dsSurvivalClient::ds.coxphSLMAassign(
      formula = "surv_object~survival$age",
      objectname = "coxph_serverside",
      datasources = conns
    )
  })

  # Get summary
  hazard_summary <- dsSurvivalClient::ds.coxphSummary(
    x = "coxph_serverside",
    datasources = conns
  )

  expected_names <- c(
    "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients",
    "conf.int", "logtest", "sctest", "rsq", "waldtest", "used.robust",
    "concordance"
  )

  expect_identical(names(hazard_summary$armadillo), expected_names)
})
