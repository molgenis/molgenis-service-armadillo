# Setup
test_name <- "assigning"
folder <- "2_1-core-1_0"
table <- "nonrep"
object <- "nonrep"
variable <- "coh_country"

test_that("assign table returns data.frame", {
  do_skip_test(test_name)
  datashield.assign.table(release_env$conns, table, sprintf("%s/%s/%s", release_env$project1, folder, table))
  symbols <- datashield.symbols(release_env$conns)
  expect_true(table %in% symbols$armadillo)
})

test_that("assign expression succeeds", {
  do_skip_test(test_name)
  expect_no_error(
    datashield.assign.expr(release_env$conns, "x", expr = as.symbol(paste0(object, "$", variable)))
  )
})
