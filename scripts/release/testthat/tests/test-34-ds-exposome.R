# test-34-ds-exposome.R - dsExposome package tests
#
# These tests verify that dsExposome functions work correctly.

# Setup: ensure researcher connection is established (resources mode - no table download)
ensure_researcher_login()
ensure_resources_uploaded()

# Skip all tests if ds-exposome is excluded
skip_if_excluded("ds-exposome")

# Load the exposome client library
library(dsExposomeClient)

# Skip if in admin mode or resourcer not available
config <- test_config
if (config$ADMIN_MODE) {
  skip("Cannot test exposome with basic authentication")
}
if (!"resourcer" %in% test_env$profile_info$packageWhitelist) {
  skip(sprintf("Resourcer not available for profile: %s", config$profile))
}

# Define exposome reference data
exposome_ref <- tibble::tribble(
  ~file_name, ~path, ~url, ~object_name, ~format,
  "exposures.csv", file.path(config$test_file_path, "exposures.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/exposures.csv", "exposures", "csv",
  "description.csv", file.path(config$test_file_path, "description.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/description.csv", "description", "csv",
  "phenotypes.csv", file.path(config$test_file_path, "phenotypes.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/phenotypes.csv", "phenotypes", "csv",
  "exposomeSet.RData", file.path(config$test_file_path, "exposomeSet.RData"), "https://github.com/isglobal-brge/brge_data_large/raw/master/data/exposomeSet.Rdata", "exposomeSet", "RData"
)

# Setup exposome data once for all tests
setup_exposome_data <- function() {
  if (isTRUE(test_env$exposome_setup)) {
    return(invisible(TRUE))
  }


  # Set DM permissions
  set_dm_permissions(
    user = config$user,
    admin_pwd = config$admin_pwd,
    required_projects = list(project),
    interactive = config$interactive,
    update_auto = config$update_auto,
    url = config$armadillo_url
  )

  # Download resources
  download_many_sources(ref = exposome_ref)

  # Upload resources
  upload_many_sources(
    project = project,
    ref = exposome_ref,
    url = config$armadillo_url,
    folder = "exposome",
    token = test_env$token,
    auth_type = config$auth_type
  )

  # Create resources
  exposome_resources <- create_many_resources(
    ref = exposome_ref,
    folder = "exposome",
    project = project,
    url = config$armadillo_url
  )

  # Upload resources to Armadillo
  upload_many_resources(
    project = project,
    resource = exposome_resources,
    folder = "exposome",
    ref = exposome_ref
  )

  # Assign resources
  assign_many_resources(
    project = project,
    folder = "exposome",
    ref = exposome_ref,
    conns = conns
  )

  # Resolve resources as data frames
  resolve_many_resources(
    resource_names = c("description", "exposures", "phenotypes"),
    conns = conns
  )

  test_env$exposome_setup <- TRUE
  invisible(TRUE)
}

test_that("exposome data can be setup", {
  expect_no_error(setup_exposome_data())
  expect_true(test_env$exposome_setup)
})

test_that("ds.loadExposome creates object with expected class", {
  setup_exposome_data()
  ds.loadExposome(
    exposures = "exposures",
    phenotypes = "phenotypes",
    exposures.idcol = "idnum",
    phenotypes.idcol = "idnum",
    description = "description",
    description.expCol = "Exposure",
    description.famCol = "Family",
    object_name = "exposome_object",
    datasources = conns
  )

  obj_class <- dsBaseClient::ds.class("exposome_object", datasources = conns)

  expect_equal(as.character(obj_class$armadillo), "ExposomeSet")
})

test_that("ds.exposome_variables returns expected phenotype variables", {
  setup_exposome_data()
  # Ensure exposome object exists
  tryCatch({
    dsBaseClient::ds.class("exposome_object", datasources = conns)
  }, error = function(e) {
    ds.loadExposome(
      exposures = "exposures",
      phenotypes = "phenotypes",
      exposures.idcol = "idnum",
      phenotypes.idcol = "idnum",
      description = "description",
      description.expCol = "Exposure",
      description.famCol = "Family",
      object_name = "exposome_object",
      datasources = conns
    )
  })

  vars <- ds.exposome_variables("exposome_object", "phenotypes", datasources = conns)

  expected_vars <- c(
    "whistling_chest", "flu", "rhinitis", "wheezing", "birthdate",
    "sex", "age", "cbmi", "blood_pre"
  )

  expect_identical(vars$armadillo, expected_vars)
})

test_that("ds.exposome_summary returns expected names", {
  setup_exposome_data()
  var_summary <- ds.exposome_summary("exposome_object", "AbsPM25", datasources = conns)

  expected_names <- c("class", "length", "quantiles & mean")

  expect_identical(names(var_summary$armadillo), expected_names)
})

test_that("ds.familyNames returns expected family names", {
  setup_exposome_data()
  vars <- ds.familyNames("exposome_object", datasources = conns)

  expected_families <- c(
    "Air Pollutants", "Metals", "PBDEs", "Organochlorines", "Bisphenol A",
    "Water Pollutants", "Built Environment", "Cotinine", "Home Environment",
    "Phthalates", "Noise", "PFOAs", "Temperature"
  )

  expect_identical(vars$armadillo, expected_families)
})

test_that("ds.tableMissings returns expected structure", {
  setup_exposome_data()
  missing_summary <- ds.tableMissings("exposome_object", set = "exposures", datasources = conns)

  expected_names <- c("pooled", "set", "output")

  expect_identical(names(missing_summary), expected_names)
})

test_that("ds.plotMissings returns ggplot object", {
  setup_exposome_data()
  missing_summary <- ds.tableMissings("exposome_object", set = "exposures", datasources = conns)
  missing_plot <- ds.plotMissings(missing_summary)

  expect_true(inherits(missing_plot$pooled, "ggplot"))
})

test_that("ds.normalityTest returns expected names", {
  setup_exposome_data()
  nm <- ds.normalityTest("exposome_object", datasources = conns)

  expected_names <- c("exposure", "normality", "p.value")

  expect_identical(names(nm$armadillo), expected_names)
})

test_that("ds.exposure_histogram returns expected names", {
  setup_exposome_data()
  hist <- ds.exposure_histogram("exposome_object", "AbsPM25", datasources = conns)

  expected_names <- c("breaks", "counts", "density", "mids", "xname", "equidist")

  expect_identical(names(hist), expected_names)
})

test_that("ds.imputation creates object with expected class", {
  setup_exposome_data()
  ds.imputation("exposome_object", "exposome_object_imputed", datasources = conns)

  obj_class <- dsBaseClient::ds.class("exposome_object_imputed", datasources = conns)

  expect_equal(as.character(obj_class$armadillo), "ExposomeSet")
})

test_that("ds.exwas returns expected class", {
  setup_exposome_data()
  exwas_results <- ds.exwas(
    "blood_pre ~ sex",
    Set = "exposome_object",
    family = "gaussian",
    type = "pooled",
    datasources = conns
  )

  expect_identical(class(exwas_results), c("list", "dsExWAS_pooled"))
})

test_that("ds.exposome_correlation returns expected dimensions", {
  setup_exposome_data()
  exposome_cor <- ds.exposome_correlation(
    "exposome_object",
    c("Metals", "Noise"),
    datasources = conns
  )[[1]][[1]]$`Correlation Matrix`[1:5, 1:5]

  expect_identical(dim(exposome_cor), as.integer(c(5, 5)))
})
