library(MolgenisArmadillo)
library(purrr)

# Setup
test_name <- "setup-resources"

test_that("upload resource file", {
  do_skip_test(test_name)
  upload_resource(folder = "ewas", file_name = "gse66351_1.rda")
  succeed()
})

test_that("create and upload resource GSE66351_1", {
  do_skip_test(test_name)
  resGSE1 <- create_resource(
    folder = "ewas",
    file_name = "gse66351_1.rda",
    resource_name = "GSE66351_1",
    format = "ExpressionSet"
  )

  armadillo.upload_resource(
    project = release_env$project1,
    folder = "ewas",
    resource = resGSE1,
    name = "GSE66351_1"
  )
  succeed()
})

test_that("verify resource was uploaded", {
  do_skip_test(test_name)
  all_resources <- armadillo.list_resources(release_env$project1)
  expected_resource <- sprintf("%s/ewas/GSE66351_1", release_env$project1)
  expect_true(expected_resource %in% all_resources,
    info = sprintf("Resource %s not found. Available: %s",
                   expected_resource, paste(all_resources, collapse=", ")))
})

# Exposome resources (for test-16)
test_that("upload exposome resources", {
  do_skip_test(test_name)
  do_skip_test("ds-exposome")
  upload_many_sources(ref = release_env$exposome_ref, folder = "exposome")
  exposome_resources <- create_many_resources(ref = release_env$exposome_ref, folder = "exposome")
  upload_many_resources(resource = exposome_resources, folder = "exposome", ref = release_env$exposome_ref)
  succeed()
})

# Omics resources (for test-17)
test_that("upload omics resources", {
  do_skip_test(test_name)
  do_skip_test("ds-omics")
  upload_many_sources(ref = release_env$omics_ref, folder = "omics")
  omics_resources <- create_many_resources(ref = release_env$omics_ref, folder = "omics")
  upload_many_resources(resource = omics_resources, folder = "omics", ref = release_env$omics_ref)
  succeed()
})
