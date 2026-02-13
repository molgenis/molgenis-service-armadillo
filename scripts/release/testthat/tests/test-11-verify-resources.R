# Setup
test_name <- "verify-resources"
resource_path <- "ewas/GSE66351_1"

test_that("verify see resource", {
  do_skip_test(test_name)
  skip_if_no_resources()
  full_resource_path <- sprintf("%s/%s", release_env$project1, resource_path)
  expect_true(full_resource_path %in% datashield.resources(conns = release_env$conns)$armadillo)
})

test_that("verify assign resource", {
  do_skip_test(test_name)
  skip_if_no_resources()
  full_resource_path <- sprintf("%s/%s", release_env$project1, resource_path)
  datashield.assign.resource(release_env$conns, resource = full_resource_path, symbol = "eSet_0y_EUR")
  resource_class <- ds.class("eSet_0y_EUR", datasources = release_env$conns)
  expected <- c("RDataFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
  expect_length(setdiff(resource_class$testserver, expected), 0)
})
#       verify_assign_expression() FIX ME: ISSUE #699
