library(dsExposomeClient)
library(purrr)

source("test-cases/download-resources.R")
source("test-cases/upload-resource.R")

verify_load_exposome_class <- function() {
  cli_alert_info("Checking ds.loadExposome")
  ds.loadExposome(
    exposures = "exposures", phenotypes = "phenotypes", exposures.idcol = "idnum",
    phenotypes.idcol = "idnum", description = "description", description.expCol = "Exposure",
    description.famCol = "Family", object_name = "exposome_object",
    datasources = release_env$conns
  )
  obj_class <- ds.class("exposome_object", datasources = release_env$conns)
  expect_identical(as.character(obj_class$armadillo), "ExposomeSet")
}

verify_exposome_variables <- function() {
  cli_alert_info("Checking ds.exposome_variables")
  vars <- ds.exposome_variables("exposome_object", "phenotypes", datasources = release_env$conns)
  expect_identical(vars$armadillo,
    c("whistling_chest", "flu", "rhinitis", "wheezing", "birthdate", "sex", "age", "cbmi", "blood_pre"))
}

verify_exposome_summary_names <- function() {
  cli_alert_info("Checking ds.exposome_summary")
  var_summary <- ds.exposome_summary("exposome_object", "AbsPM25", datasources = release_env$conns)
  expect_identical(names(var_summary$armadillo), c("class", "length", "quantiles & mean"))
}

verify_family_names <- function() {
  cli_alert_info("Checking ds.familyNames")
  vars <- ds.familyNames("exposome_object", datasources = release_env$conns)
  expect_identical(vars$armadillo, c(
    "Air Pollutants", "Metals", "PBDEs", "Organochlorines", "Bisphenol A", "Water Pollutants",
    "Built Environment", "Cotinine", "Home Environment", "Phthalates", "Noise", "PFOAs", "Temperature"
  ))
}

verify_table_missings_names <- function(missing_summary) {
  cli_alert_info("Checking ds.tableMissings")
  expect_identical(names(missing_summary), c("pooled", "set", "output"))
}

verify_plot_missings_names <- function(missing_summary) {
  cli_alert_info("Checking ds.plotMissings")
  missing_plot <- ds.plotMissings(missing_summary, datasources = release_env$conns)
  expect_true(inherits(missing_plot$pooled, "ggplot"))
}

verify_normality_test_names <- function() {
  cli_alert_info("Checking ds.normalityTest")
  nm <- ds.normalityTest("exposome_object", datasources = release_env$conns)
  expect_identical(names(nm$armadillo), c("exposure", "normality", "p.value"))
}

verify_exposure_histogram_names <- function() {
  cli_alert_info("Checking ds.exposure_histogram")
  hist <- ds.exposure_histogram("exposome_object", "AbsPM25", datasources = release_env$conns)
  expect_identical(names(hist), c("breaks", "counts", "density", "mids", "xname", "equidist"))
}

verify_imputation <- function() {
  cli_alert_info("Checking ds.imputation")
  ds.imputation("exposome_object", "exposome_object_imputed", datasources = release_env$conns)
  obj_class <- ds.class("exposome_object_imputed", datasources = release_env$conns)
  expect_identical(as.character(obj_class$armadillo), "ExposomeSet")
}

verify_exwas <- function(exwas_results) {
  cli_alert_info("Checking ds.exwas")
  expect_identical(class(exwas_results), c("list", "dsExWAS_pooled"))
}

verify_exwas_plot <- function(exwas_results) {
  cli_alert_info("Checking ds.exwas plot")
  exwas_plot <- ds.plotExwas(exwas_results, type = "effect")
  expect_identical(class(exwas_plot), c("gg", "ggplot"))
}

verify_pca_class <- function() {
  cli_alert_info("Checking ds.exposome_pca")
  ds.exposome_pca("exposome_object", fam = c("Metals", "Noise"), datasources = release_env$conns)
  pca_class <- ds.class("ds.exposome_pca.Results", datasources = release_env$conns)
  expect_identical(as.character(pca_class), "ExposomePCA")
}

verify_pca_plot_class <- function() {
  cli_alert_info("Checking ds.exposome_pca_plot")
  pca_plot <- ds.exposome_pca_plot("ds.exposome_pca.Results", set = "all", method = 1, k = 3, noise = 5,
                                    datasources = release_env$conns)
  expect_identical(class(pca_plot), c("gtable", "gTree", "grob", "gDesc"))
}

verify_exposure_cor_dim <- function() {
  cli_alert_info("Checking ds.exposome_correlation")
  exposome_cor <- ds.exposome_correlation("exposome_object", c("Metals", "Noise"),
                                           datasources = release_env$conns)[[1]][[1]]$`Correlation Matrix`[1:5, 1:5]
  expect_identical(dim(exposome_cor), as.integer(c(5, 5)))
}

exposome_ref <- tribble(
  ~file_name, ~path, ~url, ~object_name, ~format,
  "exposures.csv", file.path(release_env$test_file_path, "exposures.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/exposures.csv", "exposures", "csv",
  "description.csv", file.path(release_env$test_file_path, "description.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/description.csv", "description", "csv",
  "phenotypes.csv", file.path(release_env$test_file_path, "phenotypes.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/phenotypes.csv", "phenotypes", "csv",
  "exposomeSet.RData", file.path(release_env$test_file_path, "exposomeSet.RData"), "https://github.com/isglobal-brge/brge_data_large/raw/master/data/exposomeSet.Rdata", "exposomeSet", "RData",
)

run_exposome_tests <- function() {
  test_name <- "xenon-exposome"

  skip_exposome <- function() {
    do_skip_test(test_name)
    skip_if(release_env$ADMIN_MODE, "Cannot test resources as admin")
    skip_if(!"resourcer" %in% release_env$profile_info$packageWhitelist,
            sprintf("resourcer not available for profile: %s", release_env$current_profile))
  }

  test_that("xenon-exposome setup", {
    skip_exposome()
    set_dm_permissions()
    download_many_sources(ref = exposome_ref)
    upload_many_sources(ref = exposome_ref, folder = "exposome")
    exposome_resources <- create_many_resources(ref = exposome_ref, folder = "exposome")
    upload_many_resources(resource = exposome_resources, folder = "exposome", ref = exposome_ref)
    assign_many_resources(folder = "exposome", ref = exposome_ref)
    resolve_many_resources(resource_names = c("description", "exposures", "phenotypes"))
    succeed()
  })

  test_that("ds.loadExposome", { skip_exposome(); verify_load_exposome_class() })
  test_that("ds.exposome_variables", { skip_exposome(); verify_exposome_variables() })
  test_that("ds.exposome_summary", { skip_exposome(); verify_exposome_summary_names() })
  test_that("ds.familyNames", { skip_exposome(); verify_family_names() })

  test_that("ds.tableMissings", {
    skip_exposome()
    missing_summary <- ds.tableMissings("exposome_object", set = "exposures", datasources = release_env$conns)
    verify_table_missings_names(missing_summary)
  })

  test_that("ds.plotMissings", {
    skip_exposome()
    missing_summary <- ds.tableMissings("exposome_object", set = "exposures", datasources = release_env$conns)
    verify_plot_missings_names(missing_summary)
  })

  test_that("ds.normalityTest", { skip_exposome(); verify_normality_test_names() })
  test_that("ds.exposure_histogram", { skip_exposome(); verify_exposure_histogram_names() })
  test_that("ds.imputation", { skip_exposome(); verify_imputation() })

  test_that("ds.exwas", {
    skip_exposome()
    exwas_results <- ds.exwas("blood_pre ~ sex", Set = "exposome_object", family = "gaussian", type = "pooled",
                               datasources = release_env$conns)
    verify_exwas(exwas_results)
  })

  test_that("ds.exposome_correlation", { skip_exposome(); verify_exposure_cor_dim() })
  # verify_exwas_plot(exwas_results) https://github.com/isglobal-brge/dsExposomeClient/issues/19
  # ds.exposome_pca("exposome_object", fam = c("Metals", "Noise")) https://github.com/isglobal-brge/dsExposomeClient/issues/20
  # verify_pca_class() See above
  # verify_pca_plot_class() See above
}
