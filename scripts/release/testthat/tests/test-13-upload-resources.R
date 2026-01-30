# test-13-upload-resources.R - Resource upload and creation tests
#
# These tests verify that:
# - Resource files can be uploaded
# - Resource definitions can be created
#
# This is a prerequisite for the resources cluster (assigning-resources,
# ds-exposome, ds-omics).

ensure_resources_downloaded()
ensure_admin_setup()

test_that("resource is created and uploaded", {
  skip_if_excluded("upload-resources")

  ensure_resources_uploaded()

  resources <- MolgenisArmadillo::armadillo.list_resources(test_env$project)
  expect_true(
    "ewas/GSE66351_1" %in% resources,
    info = "Resource ewas/GSE66351_1 should exist after upload"
  )
})
