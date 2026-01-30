# test-22-assigning-tables.R - Table and expression assignment tests
#
# These tests verify that tables and expressions can be assigned.

# Setup: ensure researcher connection is established
ensure_researcher_login_and_assign()

# Helper to check all skip conditions for this test file
skip_if_assigning_tables_excluded <- function() {
  skip_if_excluded("assigning-tables")
}

# Note: suppressMessages hides "Data in all studies were valid" which is expected behavior

test_that("table can be assigned", {
  skip_if_assigning_tables_excluded()
  # Assign a table
  suppressMessages(DSI::datashield.assign.table(
    conns,
    "test_nonrep",
    sprintf("%s/2_1-core-1_0/nonrep", project)
  ))

  # Verify it's a data frame
  datatype <- suppressMessages(dsBaseClient::ds.class(x = "test_nonrep", datasources = conns))

  expect_equal(datatype$armadillo, "data.frame")
})

test_that("expression can be assigned", {
  skip_if_assigning_tables_excluded()
  # Assign an expression (extracting a column)
  expect_no_error({
    suppressMessages(DSI::datashield.assign.expr(
      conns,
      "test_x",
      expr = as.symbol("nonrep$coh_country")
    ))
  })
})
