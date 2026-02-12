library(dsExposomeClient)
library(purrr)

# Setup
test_name <- "ds-exposome"
release_env$exposome_resolved <- c(description = FALSE, exposures = FALSE, phenotypes = FALSE)

skip_if_exposome_not_resolved <- function() {
  skip_if(!all(release_env$exposome_resolved), "Error resolving exposome resources")
}

test_that("assign exposures resource", {
  skip_if_no_resources(test_name)
  datashield.assign.resource(release_env$conns,
    resource = paste0(release_env$project1, "/exposome/exposures"), symbol = "exposures")
  resource_class <- ds.class("exposures", datasources = release_env$conns)
  expected <- c("TidyFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
  expect_identical(resource_class$armadillo, expected)
})

test_that("assign description resource", {
  skip_if_no_resources(test_name)
  datashield.assign.resource(release_env$conns,
    resource = paste0(release_env$project1, "/exposome/description"), symbol = "description")
  resource_class <- ds.class("description", datasources = release_env$conns)
  expected <- c("TidyFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
  expect_identical(resource_class$armadillo, expected)
})

test_that("assign phenotypes resource", {
  skip_if_no_resources(test_name)
  datashield.assign.resource(release_env$conns,
    resource = paste0(release_env$project1, "/exposome/phenotypes"), symbol = "phenotypes")
  resource_class <- ds.class("phenotypes", datasources = release_env$conns)
  expected <- c("TidyFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
  expect_identical(resource_class$armadillo, expected)
})

test_that("assign exposomeSet resource", {
  skip_if_no_resources(test_name)
  datashield.assign.resource(release_env$conns,
    resource = paste0(release_env$project1, "/exposome/exposomeSet"), symbol = "exposomeSet")
  resource_class <- ds.class("exposomeSet", datasources = release_env$conns)
  expected <- c("RDataFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
  expect_identical(resource_class$armadillo, expected)
})

test_that("resolve description resource", {
  skip_if_no_resources(test_name)
  datashield.assign.expr(release_env$conns, symbol = "description",
    expr = as.symbol("as.resource.data.frame(description)"))
  resource_class <- ds.class("description", datasources = release_env$conns)
  dims <- ds.dim("description", datasources = release_env$conns)[[1]]
  expect_identical(resource_class$armadillo, c("spec_tbl_df", "tbl_df", "tbl", "data.frame"))
  expect_identical(dims, c(88, 3))
  if (identical(dims, c(88, 3))) release_env$exposome_resolved[["description"]] <- TRUE
})

test_that("resolve exposures resource", {
  skip_if_no_resources(test_name)
  datashield.assign.expr(release_env$conns, symbol = "exposures",
    expr = as.symbol("as.resource.data.frame(exposures)"))
  resource_class <- ds.class("exposures", datasources = release_env$conns)
  dims <- ds.dim("exposures", datasources = release_env$conns)[[1]]
  expect_identical(resource_class$armadillo, c("spec_tbl_df", "tbl_df", "tbl", "data.frame"))
  expect_identical(dims, c(109, 89))
  if (identical(dims, c(109, 89))) release_env$exposome_resolved[["exposures"]] <- TRUE
})

test_that("resolve phenotypes resource", {
  skip_if_no_resources(test_name)
  datashield.assign.expr(release_env$conns, symbol = "phenotypes",
    expr = as.symbol("as.resource.data.frame(phenotypes)"))
  resource_class <- ds.class("phenotypes", datasources = release_env$conns)
  dims <- ds.dim("phenotypes", datasources = release_env$conns)[[1]]
  expect_identical(resource_class$armadillo, c("spec_tbl_df", "tbl_df", "tbl", "data.frame"))
  expect_identical(dims, c(109, 10))
  if (identical(dims, c(109, 10))) release_env$exposome_resolved[["phenotypes"]] <- TRUE
})

# Function tests
test_that("ds.loadExposome", {
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
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
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
  vars <- ds.exposome_variables("exposome_object", "phenotypes", datasources = release_env$conns)
  expect_identical(vars$armadillo,
    c("whistling_chest", "flu", "rhinitis", "wheezing", "birthdate", "sex", "age", "cbmi", "blood_pre"))
})

test_that("ds.exposome_summary", {
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
  var_summary <- ds.exposome_summary("exposome_object", "AbsPM25", datasources = release_env$conns)
  expect_identical(names(var_summary$armadillo), c("class", "length", "quantiles & mean"))
})

test_that("ds.familyNames", {
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
  vars <- ds.familyNames("exposome_object", datasources = release_env$conns)
  expect_identical(vars$armadillo, c(
    "Air Pollutants", "Metals", "PBDEs", "Organochlorines", "Bisphenol A", "Water Pollutants",
    "Built Environment", "Cotinine", "Home Environment", "Phthalates", "Noise", "PFOAs", "Temperature"
  ))
})

test_that("ds.tableMissings", {
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
  missing_summary <- ds.tableMissings("exposome_object", set = "exposures", datasources = release_env$conns)
  expect_identical(names(missing_summary), c("pooled", "set", "output"))
})

test_that("ds.plotMissings", {
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
  missing_summary <- ds.tableMissings("exposome_object", set = "exposures", datasources = release_env$conns)
  missing_plot <- ds.plotMissings(missing_summary, datasources = release_env$conns)
  expect_true(inherits(missing_plot$pooled, "ggplot"))
})

test_that("ds.normalityTest", {
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
  nm <- ds.normalityTest("exposome_object", datasources = release_env$conns)
  expect_identical(names(nm$armadillo), c("exposure", "normality", "p.value"))
})

test_that("ds.exposure_histogram", {
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
  # Suppress "invalid cells" warning - ds.histogram warns even when there are no issues
  hist <- suppressWarnings(ds.exposure_histogram("exposome_object", "AbsPM25", datasources = release_env$conns))
  expect_identical(names(hist), c("breaks", "counts", "density", "mids", "xname", "equidist"))
})

test_that("ds.imputation", {
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
  ds.imputation("exposome_object", "exposome_object_imputed", datasources = release_env$conns)
  obj_class <- ds.class("exposome_object_imputed", datasources = release_env$conns)
  expect_identical(as.character(obj_class$armadillo), "ExposomeSet")
})

test_that("ds.exwas", {
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
  exwas_results <- ds.exwas("blood_pre ~ sex", Set = "exposome_object", family = "gaussian", type = "pooled",
                             datasources = release_env$conns, exposures_family = "Noise", tef = FALSE)
  expect_identical(class(exwas_results), c("list", "dsExWAS_pooled"))
})

test_that("ds.exposome_correlation", {
  skip_if_no_resources(test_name)
  skip_if_exposome_not_resolved()
  cor_result <- ds.exposome_correlation("exposome_object", c("Metals", "Noise"),
                                         datasources = release_env$conns)
  exposome_cor <- cor_result[[1]][[1]]$`Correlation Matrix`[1:5, 1:5]
  expect_identical(dim(exposome_cor), as.integer(c(5, 5)))
})

# Commented out due to upstream issues:
# verify_exwas_plot(exwas_results) https://github.com/isglobal-brge/dsExposomeClient/issues/19
# ds.exposome_pca("exposome_object", fam = c("Metals", "Noise")) https://github.com/isglobal-brge/dsExposomeClient/issues/20
