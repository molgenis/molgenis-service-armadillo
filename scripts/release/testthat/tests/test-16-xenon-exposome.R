library(dsExposomeClient)
library(purrr)

# Load helper functions (paths relative to release directory)
source("../../lib/upload-resource.R")
source("../../lib/create-resource.R")
source("../../lib/download-resources.R")

# Setup
test_name <- "xenon-exposome"

exposome_ref <- tribble(
  ~file_name, ~path, ~url, ~object_name, ~format,
  "exposures.csv", file.path(release_env$test_file_path, "exposures.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/exposures.csv", "exposures", "csv",
  "description.csv", file.path(release_env$test_file_path, "description.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/description.csv", "description", "csv",
  "phenotypes.csv", file.path(release_env$test_file_path, "phenotypes.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/phenotypes.csv", "phenotypes", "csv",
  "exposomeSet.RData", file.path(release_env$test_file_path, "exposomeSet.RData"), "https://github.com/isglobal-brge/brge_data_large/raw/master/data/exposomeSet.Rdata", "exposomeSet", "RData",
)

skip_exposome <- function() {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test resources as admin")
  skip_if(!"resourcer" %in% release_env$profile_info$packageWhitelist,
          sprintf("resourcer not available for profile: %s", release_env$current_profile))
}

# Setup tests
test_that("download and upload exposome sources", {
  skip_exposome()
  set_dm_permissions()
  download_many_sources(ref = exposome_ref)
  upload_many_sources(ref = exposome_ref, folder = "exposome")
  succeed()
})

test_that("create and upload exposome resources", {
  skip_exposome()
  exposome_resources <- create_many_resources(ref = exposome_ref, folder = "exposome")
  upload_many_resources(resource = exposome_resources, folder = "exposome", ref = exposome_ref)
  succeed()
})

test_that("assign and resolve exposome resources", {
  skip_exposome()
  assign_many_resources(folder = "exposome", ref = exposome_ref)
  resolve_many_resources(resource_names = c("description", "exposures", "phenotypes"))
  succeed()
})

# Function tests
test_that("ds.loadExposome", {
  skip_exposome()
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
  skip_exposome()
  vars <- ds.exposome_variables("exposome_object", "phenotypes", datasources = release_env$conns)
  expect_identical(vars$armadillo,
    c("whistling_chest", "flu", "rhinitis", "wheezing", "birthdate", "sex", "age", "cbmi", "blood_pre"))
})

test_that("ds.exposome_summary", {
  skip_exposome()
  var_summary <- ds.exposome_summary("exposome_object", "AbsPM25", datasources = release_env$conns)
  expect_identical(names(var_summary$armadillo), c("class", "length", "quantiles & mean"))
})

test_that("ds.familyNames", {
  skip_exposome()
  vars <- ds.familyNames("exposome_object", datasources = release_env$conns)
  expect_identical(vars$armadillo, c(
    "Air Pollutants", "Metals", "PBDEs", "Organochlorines", "Bisphenol A", "Water Pollutants",
    "Built Environment", "Cotinine", "Home Environment", "Phthalates", "Noise", "PFOAs", "Temperature"
  ))
})

test_that("ds.tableMissings", {
  skip_exposome()
  missing_summary <- ds.tableMissings("exposome_object", set = "exposures", datasources = release_env$conns)
  expect_identical(names(missing_summary), c("pooled", "set", "output"))
})

test_that("ds.plotMissings", {
  skip_exposome()
  missing_summary <- ds.tableMissings("exposome_object", set = "exposures", datasources = release_env$conns)
  missing_plot <- ds.plotMissings(missing_summary, datasources = release_env$conns)
  expect_true(inherits(missing_plot$pooled, "ggplot"))
})

test_that("ds.normalityTest", {
  skip_exposome()
  nm <- ds.normalityTest("exposome_object", datasources = release_env$conns)
  expect_identical(names(nm$armadillo), c("exposure", "normality", "p.value"))
})

test_that("ds.exposure_histogram", {
  skip_exposome()
  # Suppress "invalid cells" warning - ds.histogram warns even when there are no issues
  hist <- suppressWarnings(ds.exposure_histogram("exposome_object", "AbsPM25", datasources = release_env$conns))
  expect_identical(names(hist), c("breaks", "counts", "density", "mids", "xname", "equidist"))
})

test_that("ds.imputation", {
  skip_exposome()
  ds.imputation("exposome_object", "exposome_object_imputed", datasources = release_env$conns)
  obj_class <- ds.class("exposome_object_imputed", datasources = release_env$conns)
  expect_identical(as.character(obj_class$armadillo), "ExposomeSet")
})

test_that("ds.exwas", {
  skip_exposome()
  exwas_results <- ds.exwas("blood_pre ~ sex", Set = "exposome_object", family = "gaussian", type = "pooled",
                             datasources = release_env$conns)
  expect_identical(class(exwas_results), c("list", "dsExWAS_pooled"))
})

test_that("ds.exposome_correlation", {
  skip_exposome()
  exposome_cor <- ds.exposome_correlation("exposome_object", c("Metals", "Noise"),
                                           datasources = release_env$conns)[[1]][[1]]$`Correlation Matrix`[1:5, 1:5]
  expect_identical(dim(exposome_cor), as.integer(c(5, 5)))
})

# Commented out due to upstream issues:
# verify_exwas_plot(exwas_results) https://github.com/isglobal-brge/dsExposomeClient/issues/19
# ds.exposome_pca("exposome_object", fam = c("Metals", "Noise")) https://github.com/isglobal-brge/dsExposomeClient/issues/20
