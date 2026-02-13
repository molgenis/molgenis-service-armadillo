library(MolgenisArmadillo)

# Setup
test_name <- "setup-resources"

test_that("upload resource file", {
  do_skip_test(test_name)
  skip_if(resource_exists(release_env$project1, "ewas", "GSE66351_1"), "Resource file already uploaded")
  upload_resource(folder = "ewas", file_name = "gse66351_1.rda")
  succeed()
})

test_that("create and upload resource GSE66351_1", {
  do_skip_test(test_name)
  skip_if(resource_exists(release_env$project1, "ewas", "GSE66351_1"), "Resource already uploaded")
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
