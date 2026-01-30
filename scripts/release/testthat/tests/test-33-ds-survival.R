# test-32-ds-survival.R - dsSurvival package tests
#
# These tests verify that dsSurvival functions work correctly.

# Setup: ensure researcher connection is established
ensure_researcher_login_and_assign()

# Load the survival client library
library(dsSurvivalClient)

# Helper to check all skip conditions for this test file
skip_if_ds_survival_excluded <- function() {
  skip_if_excluded("ds-survival")
}

# Helper to assign survival data
# Note: suppressMessages hides "Data in all studies were valid" which is expected behavior
assign_survival_data <- function() {
  data_path <- "/survival/veteran"

  suppressMessages(DSI::datashield.assign.table(
    conns,
    "survival",
    sprintf("%s%s", project, data_path)
  ))
}

test_that("survival data can be assigned", {
  skip_if_ds_survival_excluded()
  assign_survival_data()

  datatype <- suppressMessages(dsBaseClient::ds.class(x = "survival", datasources = conns))

  expect_equal(datatype$armadillo, "data.frame")
})

test_that("ds.Surv creates object with expected class", {
  skip_if_ds_survival_excluded()
  suppressMessages({
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
    surv_class <- dsBaseClient::ds.class("surv_object", datasources = conns)
  })

  expect_equal(surv_class$armadillo, "Surv")
})

test_that("ds.coxph.SLMA returns expected elements", {
  skip_if_ds_survival_excluded()
  suppressMessages({
    # Ensure survival object exists
    tryCatch(
      {
        dsBaseClient::ds.class("surv_object")
      },
      error = function(e) {
        assign_survival_data()
        dsSurvivalClient::ds.Surv(
          time = "survival$time",
          event = "survival$status",
          objectname = "surv_object",
          datasources = conns
        )
      }
    )

    # Run Cox regression
    cox_output <- dsSurvivalClient::ds.coxph.SLMA(
      formula = "surv_object~survival$age",
      datasources = conns
    )
  })

  expected_names <- c(
    "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients",
    "conf.int", "logtest", "sctest", "rsq", "waldtest", "used.robust",
    "concordance"
  )

  expect_identical(names(cox_output$armadillo), expected_names)
})

test_that("ds.coxphSLMAassign creates object with expected class", {
  skip_if_ds_survival_excluded()
  suppressMessages({
    # Ensure survival object exists
    tryCatch(
      {
        dsBaseClient::ds.class("surv_object", datasources = conns)
      },
      error = function(e) {
        assign_survival_data()
        dsSurvivalClient::ds.Surv(
          time = "survival$time",
          event = "survival$status",
          objectname = "surv_object",
          datasources = conns
        )
      }
    )

    # Assign Cox model object
    dsSurvivalClient::ds.coxphSLMAassign(
      formula = "surv_object~survival$age",
      objectname = "coxph_serverside",
      datasources = conns
    )

    # Check class
    cox_class <- dsBaseClient::ds.class("coxph_serverside", datasources = conns)
  })

  expect_equal(cox_class$armadillo, "coxph")
})

test_that("ds.cox.zphSLMA returns expected elements", {
  skip_if_ds_survival_excluded()
  suppressMessages({
    # Ensure coxph object exists
    tryCatch(
      {
        dsBaseClient::ds.class("coxph_serverside", datasources = conns)
      },
      error = function(e) {
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
      }
    )

    # Test proportional hazards assumption
    hazard_assumption <- dsSurvivalClient::ds.cox.zphSLMA(
      fit = "coxph_serverside",
      datasources = conns
    )
  })

  expected_names <- c("table", "var", "transform", "call")

  expect_identical(names(hazard_assumption$armadillo), expected_names)
})

test_that("ds.coxphSummary returns expected elements", {
  skip_if_ds_survival_excluded()
  suppressMessages({
    # Ensure coxph object exists
    tryCatch(
      {
        dsBaseClient::ds.class("coxph_serverside", datasources = conns)
      },
      error = function(e) {
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
      }
    )

    # Get summary
    hazard_summary <- dsSurvivalClient::ds.coxphSummary(
      x = "coxph_serverside",
      datasources = conns
    )
  })

  expected_names <- c(
    "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients",
    "conf.int", "logtest", "sctest", "rsq", "waldtest", "used.robust",
    "concordance"
  )

  expect_identical(names(hazard_summary$armadillo), expected_names)
})
