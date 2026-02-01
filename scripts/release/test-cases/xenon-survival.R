library(dsSurvivalClient)

assign_survival_data <- function(data_path) {
  cli_alert_info(sprintf("Assigning table: [%s%s]", release_env$project1, data_path))
  datashield.assign.table(release_env$conns, "survival", sprintf("%s%s", release_env$project1, data_path))
}

run_survival_tests <- function(data_path) {
  test_name <- "xenon-survival"

  test_that("survival setup and ds.Surv creates correct class", {
    do_skip_test(test_name)
    assign_survival_data(data_path)
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
    do_skip_test(test_name)
    cox_output <- dsSurvivalClient::ds.coxph.SLMA(formula = "surv_object~survival$age",
                                                    datasources = release_env$conns)
    expected_names <- c(
      "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients", "conf.int", "logtest",
      "sctest", "rsq", "waldtest", "used.robust", "concordance"
    )
    expect_identical(names(cox_output$armadillo), expected_names)
  })

  test_that("ds.coxphSLMAassign creates correct class", {
    do_skip_test(test_name)
    dsSurvivalClient::ds.coxphSLMAassign(
      formula = "surv_object~survival$age",
      objectname = "coxph_serverside",
      datasources = release_env$conns
    )
    cox_class <- ds.class("coxph_serverside", datasources = release_env$conns)
    expect_equal(cox_class$armadillo, "coxph")
  })

  test_that("ds.cox.zphSLMA returns expected elements", {
    do_skip_test(test_name)
    hazard_assumption <- dsSurvivalClient::ds.cox.zphSLMA(fit = "coxph_serverside",
                                                           datasources = release_env$conns)
    expected_names <- c("table", "var", "transform", "call")
    expect_identical(names(hazard_assumption$armadillo), expected_names)
  })

  test_that("ds.coxphSummary returns expected elements", {
    do_skip_test(test_name)
    hazard_summary <- dsSurvivalClient::ds.coxphSummary(x = "coxph_serverside",
                                                         datasources = release_env$conns)
    expected_names <- c(
      "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients",
      "conf.int", "logtest", "sctest", "rsq", "waldtest", "used.robust", "concordance"
    )
    expect_identical(names(hazard_summary$armadillo), expected_names)
  })
}
