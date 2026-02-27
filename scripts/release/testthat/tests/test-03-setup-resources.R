library(MolgenisArmadillo)
library(purrr)

# Setup
test_name <- "setup-resources"

# EWAS resources (for test-09)
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

# Exposome resources (for test-14)
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

# Omics resources (for test-15)
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

# ---- Resource permission test data ----
# Creates two extra projects with resources for cross-project permission
# tests in test-08-resources.R. Researcher access is configured there.

test_that("create projects for resource permission tests", {
  do_skip_test(test_name)

  release_env$res_project_a <- generate_random_project_name()
  release_env$res_project_b <- generate_random_project_name()

  armadillo.create_project(release_env$res_project_a)
  armadillo.create_project(release_env$res_project_b)

  # Upload the same data file to both projects
  rda_file_body <- httr::upload_file(release_env$rda_dir)
  post_resource_to_api(
    release_env$res_project_a, release_env$admin_pwd, "basic",
    rda_file_body, "ewas", "gse66351_1.rda", release_env$armadillo_url
  )
  post_resource_to_api(
    release_env$res_project_b, release_env$admin_pwd, "basic",
    rda_file_body, "ewas", "gse66351_1.rda", release_env$armadillo_url
  )

  succeed()
})

test_that("upload old path resources for permission tests", {
  do_skip_test(test_name)
  skip_if(is.null(release_env$res_project_a), "Resource project setup failed")

  # (i) descriptor in project-a, data in project-a
  armadillo.upload_resource(
    project = release_env$res_project_a, folder = "ewas",
    name = "old_i",
    resource = create_resource(
      folder = "ewas", file_name = "gse66351_1.rda",
      resource_name = "old_i", format = "ExpressionSet",
      target_project = release_env$res_project_a
    )
  )

  # (ii) descriptor in project-b, data in project-b
  armadillo.upload_resource(
    project = release_env$res_project_b, folder = "ewas",
    name = "old_ii",
    resource = create_resource(
      folder = "ewas", file_name = "gse66351_1.rda",
      resource_name = "old_ii", format = "ExpressionSet",
      target_project = release_env$res_project_b
    )
  )

  # (iii) descriptor in project-a, data in project-b
  armadillo.upload_resource(
    project = release_env$res_project_a, folder = "ewas",
    name = "old_iii",
    resource = create_resource(
      folder = "ewas", file_name = "gse66351_1.rda",
      resource_name = "old_iii", format = "ExpressionSet",
      target_project = release_env$res_project_b
    )
  )

  # (iv) descriptor in project-b, data in project-a
  armadillo.upload_resource(
    project = release_env$res_project_b, folder = "ewas",
    name = "old_iv",
    resource = create_resource(
      folder = "ewas", file_name = "gse66351_1.rda",
      resource_name = "old_iv", format = "ExpressionSet",
      target_project = release_env$res_project_a
    )
  )

  succeed()
})

test_that("upload new path resources for permission tests", {
  do_skip_test(test_name)
  skip_if(is.null(release_env$res_project_a), "Resource project setup failed")

  # (i) descriptor in project-a, data in project-a
  armadillo.upload_resource(
    project = release_env$res_project_a, folder = "ewas",
    name = "new_i",
    resource = create_resource(
      folder = "ewas", file_name = "gse66351_1.rda",
      resource_name = "new_i", format = "ExpressionSet",
      target_project = release_env$res_project_a, use_rawfiles = TRUE
    )
  )

  # (ii) descriptor in project-b, data in project-b
  armadillo.upload_resource(
    project = release_env$res_project_b, folder = "ewas",
    name = "new_ii",
    resource = create_resource(
      folder = "ewas", file_name = "gse66351_1.rda",
      resource_name = "new_ii", format = "ExpressionSet",
      target_project = release_env$res_project_b, use_rawfiles = TRUE
    )
  )

  # (iii) descriptor in project-a, data in project-b
  armadillo.upload_resource(
    project = release_env$res_project_a, folder = "ewas",
    name = "new_iii",
    resource = create_resource(
      folder = "ewas", file_name = "gse66351_1.rda",
      resource_name = "new_iii", format = "ExpressionSet",
      target_project = release_env$res_project_b, use_rawfiles = TRUE
    )
  )

  # (iv) descriptor in project-b, data in project-a
  armadillo.upload_resource(
    project = release_env$res_project_b, folder = "ewas",
    name = "new_iv",
    resource = create_resource(
      folder = "ewas", file_name = "gse66351_1.rda",
      resource_name = "new_iv", format = "ExpressionSet",
      target_project = release_env$res_project_a, use_rawfiles = TRUE
    )
  )

  succeed()
})
