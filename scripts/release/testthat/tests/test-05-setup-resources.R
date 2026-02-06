library(MolgenisArmadillo)

# Load helper functions (paths relative to release directory)
source("../../lib/upload-resource.R")
source("../../lib/create-resource.R")

# Setup
test_name <- "setup-resources"

test_that("upload and create resource GSE66351_1", {
  do_skip_test(test_name)

  upload_resource(folder = "ewas", file_name = "gse66351_1.rda")

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

  # Verify resource was uploaded successfully
  all_resources <- armadillo.list_resources(release_env$project1)
  expected_resource <- sprintf("%s/ewas/GSE66351_1", release_env$project1)
  expect_true(expected_resource %in% all_resources,
    info = sprintf("Resource %s not found. Available: %s",
                   expected_resource, paste(all_resources, collapse=", ")))
})
