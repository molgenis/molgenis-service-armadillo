library(dsSurvivalClient)

# Setup
test_name <- "xenon-survival"
data_path <- "/survival/veteran"

# Assign survival data (setup, not a test)
if (!test_name %in% release_env$skip_tests) {
  invisible(datashield.assign.table(release_env$conns, "survival", sprintf("%s%s", release_env$project1, data_path)))
}

test_that("ds.Surv creates correct class", {
  do_skip_test(test_name)
  out <- capture.output({
    invisible(dsSurvivalClient::ds.Surv(
      time = "survival$time",
      event = "survival$status",
      objectname = "surv_object",
      datasources = release_env$conns
    ))
    surv_class <- ds.class("surv_object", datasources = release_env$conns)
  })
  cat("CAPTURED:", out, "\n", file = "/tmp/trace.txt", append = TRUE, sep = "|")
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
#
# test_that("ds.coxphSLMAassign creates correct class", {
#   do_skip_test(test_name)
#   invisible(dsSurvivalClient::ds.coxphSLMAassign(
#     formula = "surv_object~survival$age",
#     objectname = "coxph_serverside",
#     datasources = release_env$conns
#   ))
#   cox_class <- ds.class("coxph_serverside", datasources = release_env$conns)
#   expect_equal(cox_class$armadillo, "coxph")
# })
#
# test_that("ds.cox.zphSLMA returns expected elements", {
#   do_skip_test(test_name)
#   hazard_assumption <- dsSurvivalClient::ds.cox.zphSLMA(fit = "coxph_serverside",
#                                                          datasources = release_env$conns)
#   expected_names <- c("table", "var", "transform", "call")
#   expect_identical(names(hazard_assumption$armadillo), expected_names)
# })
#
# test_that("ds.coxphSummary returns expected elements", {
#   do_skip_test(test_name)
#   hazard_summary <- dsSurvivalClient::ds.coxphSummary(x = "coxph_serverside",
#                                                        datasources = release_env$conns)
#   expected_names <- c(
#     "call", "fail", "na.action", "n", "loglik", "nevent", "coefficients",
#     "conf.int", "logtest", "sctest", "rsq", "waldtest", "used.robust", "concordance"
#   )
#   expect_identical(names(hazard_summary$armadillo), expected_names)
# })
