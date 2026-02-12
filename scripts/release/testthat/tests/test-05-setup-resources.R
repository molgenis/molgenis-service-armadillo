library(MolgenisArmadillo)
library(purrr)

# Setup
test_name <- "setup-resources"

# EWAS resources (for test-11)
test_that("upload ewas sources", {
  do_skip_test(test_name)
  upload_resource(folder = "ewas", file_name = "gse66351_1.rda")
  succeed()
})

test_that("create and upload ewas resources", {
  do_skip_test(test_name)
  ewas_resource <- create_resource(
    folder = "ewas",
    file_name = "gse66351_1.rda",
    resource_name = "GSE66351_1",
    format = "ExpressionSet"
  )
  armadillo.upload_resource(
    project = release_env$project1,
    folder = "ewas",
    resource = ewas_resource,
    name = "GSE66351_1"
  )
  all_resources <- armadillo.list_resources(release_env$project1)
  expected <- sprintf("%s/ewas/GSE66351_1", release_env$project1)
  expect_true(all(expected %in% all_resources))
})

# Exposome resources (for test-16)
test_that("upload exposome sources", {
  do_skip_test(test_name)
  do_skip_test("ds-exposome")
  upload_many_sources(ref = release_env$exposome_ref, folder = "exposome")
  succeed()
})

test_that("create and upload exposome resources", {
  do_skip_test(test_name)
  do_skip_test("ds-exposome")
  exposome_resources <- create_many_resources(ref = release_env$exposome_ref, folder = "exposome")
  upload_many_resources(resource = exposome_resources, folder = "exposome", ref = release_env$exposome_ref)
  all_resources <- armadillo.list_resources(release_env$project1)
  expected <- sprintf("%s/exposome/%s", release_env$project1, release_env$exposome_ref$object_name)
  expect_true(all(expected %in% all_resources))
})

# Omics resources (for test-17)
test_that("upload omics sources", {
  do_skip_test(test_name)
  do_skip_test("ds-omics")
  upload_many_sources(ref = release_env$omics_ref, folder = "omics")
  succeed()
})

test_that("create and upload omics resources", {
  do_skip_test(test_name)
  do_skip_test("ds-omics")
  omics_resources <- create_many_resources(ref = release_env$omics_ref, folder = "omics")
  upload_many_resources(resource = omics_resources, folder = "omics", ref = release_env$omics_ref)
  all_resources <- armadillo.list_resources(release_env$project1)
  expected <- sprintf("%s/omics/%s", release_env$project1, release_env$omics_ref$object_name)
  expect_true(all(expected %in% all_resources))
})
