library(dsExposomeClient)
library(purrr)

# Setup
test_name <- "dsExposome"

# Setup tests
test_that("upload exposome sources", {
  skip_ds_resource_test(test_name)
  set_dm_permissions()
  upload_many_sources(ref = release_env$exposome_ref, folder = "exposome")
  succeed()
})

test_that("create and upload exposome resources", {
  skip_ds_resource_test(test_name)
  exposome_resources <- create_many_resources(ref = release_env$exposome_ref, folder = "exposome")
  upload_many_resources(resource = exposome_resources, folder = "exposome", ref = release_env$exposome_ref)
  succeed()
})

test_that("assign and resolve exposome resources", {
  skip_ds_resource_test(test_name)
  assign_many_resources(folder = "exposome", ref = release_env$exposome_ref)
  resolve_many_resources(resource_names = c("description", "exposures", "phenotypes"))
  succeed()
})

# Function tests
test_that("ds.loadExposome", {
  skip_ds_resource_test(test_name)
  ds.loadExposome(
    exposures = "exposures", phenotypes = "phenotypes", exposures.idcol = "idnum",
    phenotypes.idcol = "idnum", description = "description", description.expCol = "Exposure",
    description.famCol = "Family", object_name = "exposome_object",
    datasources = release_env$conns
  )
  obj_class <- ds.class("exposome_object", datasources = release_env$conns)
  expect_identical(as.character(obj_class$armadillo), "ExposomeSet")
})

test_that("ds.exposome_variables", {
  skip_ds_resource_test(test_name)
  vars <- ds.exposome_variables("exposome_object", "phenotypes", datasources = release_env$conns)
  expect_identical(vars$armadillo,
    c("whistling_chest", "flu", "rhinitis", "wheezing", "birthdate", "sex", "age", "cbmi", "blood_pre"))
})

test_that("ds.exposome_summary", {
  skip_ds_resource_test(test_name)
  var_summary <- ds.exposome_summary("exposome_object", "AbsPM25", datasources = release_env$conns)
  expect_identical(names(var_summary$armadillo), c("class", "length", "quantiles & mean"))
})

test_that("ds.familyNames", {
  skip_ds_resource_test(test_name)
  vars <- ds.familyNames("exposome_object", datasources = release_env$conns)
  expect_identical(vars$armadillo, c(
    "Air Pollutants", "Metals", "PBDEs", "Organochlorines", "Bisphenol A", "Water Pollutants",
    "Built Environment", "Cotinine", "Home Environment", "Phthalates", "Noise", "PFOAs", "Temperature"
  ))
})

test_that("ds.tableMissings", {
  skip_ds_resource_test(test_name)
  missing_summary <- ds.tableMissings("exposome_object", set = "exposures", datasources = release_env$conns)
  expect_identical(names(missing_summary), c("pooled", "set", "output"))
})

test_that("ds.plotMissings", {
  skip_ds_resource_test(test_name)
  missing_summary <- ds.tableMissings("exposome_object", set = "exposures", datasources = release_env$conns)
  missing_plot <- ds.plotMissings(missing_summary, datasources = release_env$conns)
  expect_true(inherits(missing_plot$pooled, "ggplot"))
})

test_that("ds.normalityTest", {
  skip_ds_resource_test(test_name)
  nm <- ds.normalityTest("exposome_object", datasources = release_env$conns)
  expect_identical(names(nm$armadillo), c("exposure", "normality", "p.value"))
})

test_that("ds.exposure_histogram", {
  skip_ds_resource_test(test_name)
  # Suppress "invalid cells" warning - ds.histogram warns even when there are no issues
  hist <- suppressWarnings(ds.exposure_histogram("exposome_object", "AbsPM25", datasources = release_env$conns))
  expect_identical(names(hist), c("breaks", "counts", "density", "mids", "xname", "equidist"))
})

test_that("ds.imputation", {
  skip_ds_resource_test(test_name)
  ds.imputation("exposome_object", "exposome_object_imputed", datasources = release_env$conns)
  obj_class <- ds.class("exposome_object_imputed", datasources = release_env$conns)
  expect_identical(as.character(obj_class$armadillo), "ExposomeSet")
})

test_that("ds.exwas", {
  skip_ds_resource_test(test_name)
  exwas_results <- ds.exwas("blood_pre ~ sex", Set = "exposome_object", family = "gaussian", type = "pooled",
                             datasources = release_env$conns, exposures_family = "Noise", tef = FALSE)
  expect_identical(class(exwas_results), c("list", "dsExWAS_pooled"))
})

test_that("ds.exposome_correlation", {
  skip_ds_resource_test(test_name)
    cor_result <- ds.exposome_correlation("exposome_object", c("Metals", "Noise"),
                                           datasources = release_env$conns)
  exposome_cor <- cor_result[[1]][[1]]$`Correlation Matrix`[1:5, 1:5]
  expect_identical(dim(exposome_cor), as.integer(c(5, 5)))
})

# Commented out due to upstream issues:
# verify_exwas_plot(exwas_results) https://github.com/isglobal-brge/dsExposomeClient/issues/19
# ds.exposome_pca("exposome_object", fam = c("Metals", "Noise")) https://github.com/isglobal-brge/dsExposomeClient/issues/20
