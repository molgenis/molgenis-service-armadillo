library(dsSurvivalClient)

create_survival_object <- function() {
  cli_alert_info("Creating survival object")
  dsSurvivalClient::ds.Surv(
    time = "survival$time",
    event = "survival$status",
    objectname = "surv_object",
    datasources = release_env$conns
  )
}

verify_survival_class <- function() {
  cli_alert_info("Checking ds.Surv")
  surv_class <- ds.class("surv_object", datasources = release_env$conns)

  if (surv_class$armadillo == "Surv") {
    cli_alert_success("ds.survival passed")
  } else {
    cli_alert_danger("ds.survival failed")
    exit_test("ds.survival did not create a serverside object with the expected class")
  }
}

verify_cox_output <- function() {
  cli_alert_info("Checking ds.coxph.SLMA")
  cox_output <- dsSurvivalClient::ds.coxph.SLMA(formula = "surv_object~survival$age",
                                                  datasources = release_env$conns)
  expected_names <- c(
    "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients", "conf.int", "logtest",
    "sctest", "rsq", "waldtest", "used.robust", "concordance"
  )

  if (identical(names(cox_output$armadillo), expected_names)) {
    cli_alert_success("ds.coxph.SLMA passed")
  } else {
    cli_alert_danger("ds.coxph.SLMA failed")
    exit_test("ds.coxph.SLMA did not create a list with the expected elements")
  }
}

verify_cox_phSLMAassign_class <- function() {
  cli_alert_info("Checking ds.coxphSLMAassign")
  dsSurvivalClient::ds.coxphSLMAassign(
    formula = "surv_object~survival$age",
    objectname = "coxph_serverside",
    datasources = release_env$conns
  )

  cox_class <- ds.class("coxph_serverside", datasources = release_env$conns)

  if (cox_class$armadillo == "coxph") {
    cli_alert_success("ds.coxphSLMAassign passed")
  } else {
    cli_alert_danger("ds.coxphSLMAassign failed")
    exit_test("ds.coxphSLMAassign did not create a serverside object with the expected class")
  }
}

verify_cox_zphSLMA_object <- function() {
  cli_alert_info("Checking ds.coxphSummary")
  cli_alert_info("Checking ds.cox.zphSLMA")
  hazard_assumption <- dsSurvivalClient::ds.cox.zphSLMA(fit = "coxph_serverside",
                                                         datasources = release_env$conns)
  expected_names <- c("table", "var", "transform", "call")

  if (identical(names(hazard_assumption$armadillo), expected_names)) {
    cli_alert_success("ds.cox.zphSLMA passed")
  } else {
    cli_alert_danger("ds.cox.zphSLMA failed")
    exit_test("ds.cox.zphSLMA did not create a list with the expected elements")
  }
}

verify_cox_phsummary <- function() {
  hazard_summary <- dsSurvivalClient::ds.coxphSummary(x = "coxph_serverside",
                                                       datasources = release_env$conns)
  expected_names <- c(
    "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients",
    "conf.int", "logtest", "sctest", "rsq", "waldtest", "used.robust", "concordance"
  )

  if (identical(names(hazard_summary$armadillo), expected_names)) {
    cli_alert_success("ds.coxphSummary passed")
  } else {
    cli_alert_danger("ds.coxphSummary failed")
    exit_test("ds.coxphSummary did not create a list with the expected elements")
  }
}

assign_survival_data <- function() {
  cli_alert_info(sprintf("Assigning table: [%s/survival/veteran]", release_env$project1))
  datashield.assign.table(release_env$conns, "survival", sprintf("%s/survival/veteran", release_env$project1))
}

run_survival_tests <- function() {
  test_name <- "xenon-survival"
  if (do_skip_test(test_name)) {
    return()
  }

  assign_survival_data()
  create_survival_object()
  verify_survival_class()
  verify_cox_output()
  verify_cox_phSLMAassign_class()
  verify_cox_zphSLMA_object()
  verify_cox_phsummary()
  cli_alert_success(sprintf("%s passed!", test_name))
}
