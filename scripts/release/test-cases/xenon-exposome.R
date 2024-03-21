library(dsExposomeClient)
library(purrr)
library(tibble)

source("test-cases/download-resources.R")
source("test-cases/upload-resource.R")

verify_load_exposome_class <- function() {
  ds_function_name <- "ds.loadExposome"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.loadExposome(
    exposures = "exposures", phenotypes = "phenotypes", exposures.idcol = "idnum",
    phenotypes.idcol = "idnum", description = "description", description.expCol = "Exposure",
    description.famCol = "Family", object_name = "exposome_object"
  )
  obj_class <- ds.class("exposome_object")
  verify_output(
    function_name = ds_function_name, object = as.character(obj_class$armadillo),
    expected = "ExposomeSet", fail_msg = xenon_fail_msg$srv_class
  )
}

verify_exposome_variables <- function() {
  ds_function_name <- "ds.exposome_variables"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  vars <- ds.exposome_variables("exposome_object", "phenotypes")
  verify_output(
    function_name = ds_function_name, object = vars$armadillo,
    expected = c("whistling_chest", "flu", "rhinitis", "wheezing", "birthdate", "sex", "age", "cbmi", "blood_pre"),
    fail_msg = xenon_fail_msg$clt_vars
  )
}

verify_exposome_summary_names <- function() {
  ds_function_name <- "ds.exposome_summary"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  var_summary <- ds.exposome_summary("exposome_object", "AbsPM25")
  verify_output(
    function_name = ds_function_name, object = names(var_summary$armadillo),
    expected = c("class", "length", "quantiles & mean"),
    fail_msg = xenon_fail_msg$clt_list_names
  )
}

verify_family_names <- function() {
  ds_function_name <- "ds.familyNames"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  vars <- ds.familyNames("exposome_object")
  verify_output(
    function_name = ds_function_name, object = vars$armadillo,
    expected = c(
      "Air Pollutants", "Metals", "PBDEs", "Organochlorines", "Bisphenol A", "Water Pollutants",
      "Built Environment", "Cotinine", "Home Environment", "Phthalates", "Noise", "PFOAs", "Temperature"
    ),
    fail_msg = xenon_fail_msg$clt_list_names
  )
}

verify_table_missings_names <- function(misssing_summary) {
  ds_function_name <- "ds.tableMissings"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  verify_output(
    function_name = ds_function_name, object = names(misssing_summary),
    expected = c("pooled", "set", "output"), fail_msg = list_names_msg
  )
}

verify_plot_missings_names <- function(misssing_summary) {
  ds_function_name <- "ds.plotMissings"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  missing_plot <- ds.plotMissings(misssing_summary)
  verify_output(
    function_name = ds_function_name, object = names(missing_plot$pooled),
    expected = c(
      "data", "layers", "scales", "guides", "mapping", "theme", "coordinates",
      "facet", "plot_env", "layout", "labels"
    ),
    fail_msg = xenon_fail_msg$clt_list_names
  )
}

verify_normality_test_names <- function() {
  ds_function_name <- "ds.normalityTest"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  nm <- ds.normalityTest("exposome_object")
  verify_output(
    function_name = ds_function_name, object = names(nm$armadillo),
    expected = c("exposure", "normality", "p.value"),
    fail_msg = xenon_fail_msg$clt_list_names
  )
}

verify_exposure_histogram_names <- function() {
  ds_function_name <- "ds.exposure_histogram"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  hist <- ds.exposure_histogram("exposome_object", "AbsPM25")
  verify_output(
    function_name = ds_function_name, object = names(hist),
    expected = c("breaks", "counts", "density", "mids", "xname", "equidist"),
    fail_msg = xenon_fail_msg$clt_list_names
  )
}

verify_imputation <- function() {
  ds_function_name <- "ds.imputation"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.imputation("exposome_object", "exposome_object_imputed")
  obj_class <- ds.class("exposome_object_imputed")
  verify_output(
    function_name = ds_function_name, object = as.character(obj_class$armadillo),
    expected = "ExposomeSet", fail_msg = xenon_fail_msg$srv_class
  )
}

verify_exwas <- function(exwas_results) {
  ds_function_name <- "ds.exwas"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  verify_output(
    function_name = ds_function_name, object = class(exwas_results),
    expected = c("list", "dsExWAS_pooled"), fail_msg = xenon_fail_msg$clt_class
  )
}

verify_exwas_plot <- function(exwas_results) {
  ds_function_name <- "ds.exwas"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  exwas_plot <- ds.plotExwas(exwas_results, type = "effect")
  verify_output(
    function_name = ds_function_name, object = class(exwas_plot),
    expected = c("gg", "ggplot"), fail_msg = xenon_fail_msg$clt_class
  )
}

verify_pca_class <- function(ds_function_name) {
  ds_function_name <- "ds.exposome_pca"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.exposome_pca("exposome_object", fam = c("Metals", "Noise"))
  pca_class <- ds.class("ds.exposome_pca.Results")
  verify_output(
    function_name = ds_function_name, object = as.character(pca_class),
    expected = "ExposomePCA", fail_msg = xenon_fail_msg$clt_class
  )
}

verify_pca_plot_class <- function(ds_function_name) {
  ds_function_name <- "ds.exposome_pca_plot"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  pca_plot <- ds.exposome_pca_plot("ds.exposome_pca.Results", set = "all", method = 1, k = 3, noise = 5)
  verify_output(
    function_name = ds_function_name, object = class(pca_plot),
    expected = c("gtable", "gTree", "grob", "gDesc"), fail_msg = xenon_fail_msg$clt_class
  )
}

verify_exposure_cor_dim <- function(ds_function_name) {
  ds_function_name <- "ds.exposome_correlation"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  exposome_cor <- ds.exposome_correlation("exposome_object", c("Metals", "Noise"))[[1]][[1]]$`Correlation Matrix`[1:5, 1:5]
  verify_output(
    function_name = ds_function_name, object = dim(exposome_cor),
    expected = as.integer(c(5, 5)), fail_msg = xenon_fail_msg$clt_dim
  )
}

download_exposome_sources <- function(exposome_ref, skip_tests) {
  exposome_ref %>%
    pmap(function(path, url, ...) {
      prepare_resources(resource_path = path, url = url, skip_tests = skip_tests)
    })
}

upload_exposome_sources <- function(project, exposome_ref, url, token, auth_type, folder, file_name, skip_tests) {
  exposome_ref %>%
    pmap(function(path, file_name, ...) {
      upload_resource(project = project, rda_dir = path, url = url, token = token, folder = "exposome", file_name = file_name, auth_type = auth_type, skip_tests = NULL)
    })
}

create_exposome_resources <- function(exposome_ref, project, url, skip_tests) {
  exposome_ref %>%
    pmap(function(object_name, format, file_name, ...) {
      create_resource(target_project = project, url = url, folder = "exposome", format = format, file_name = file_name, resource_name = object_name, skip_tests)
    })
}

upload_exposome_resources <- function(project, resource, exposome_ref) {
  list(resource = resource, name = exposome_ref$object_name) %>%
    pmap(function(resource, name) {
      armadillo.upload_resource(project = project, folder = "exposome", resource = resource, name = name)
    })
}

assign_exposome_resources <- function(project, exposome_ref) {
  exposome_ref$object_name %>%
    map(function(x) {
      exp_resource_path <- paste0(project, "/exposome/", x)
      datashield.assign.resource(conns, resource = exp_resource_path, symbol = x)
    })
}

resolve_exposome_resources <- function(resource_names) {
  resource_names %>%
    map(~ datashield.assign.expr(conns, symbol = .x, expr = as.symbol(paste0("as.resource.data.frame(", .x, ")"))))
}

exposome_ref <- tribble(
  ~file_name, ~path, ~url, ~object_name, ~format,
  "exposures.csv", file.path(test_file_path, "exposures.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/exposures.csv", "exposures", "csv",
  "description.csv", file.path(test_file_path, "description.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/description.csv", "description", "csv",
  "phenotypes.csv", file.path(test_file_path, "phenotypes.csv"), "https://raw.githubusercontent.com/isglobal-brge/rexposome/master/inst/extdata/phenotypes.csv", "phenotypes", "csv",
  "exposomeSet.RData", file.path(test_file_path, "exposomeSet.RData"), "https://github.com/isglobal-brge/brge_data_large/raw/master/data/exposomeSet.Rdata", "exposomeSet", "RData",
)

run_exposome_tests <- function(project, url, token, auth_type, ADMIN_MODE, profile, profile_info, exposome_ref, skip_tests,
                               user, admin_pwd, interactive, update_auto) {
  test_name <- "xenon-exposome"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }
  if (ADMIN_MODE) {
    cli_alert_warning("Cannot test working with resources as basic authenticated admin")
  } else if (!"resourcer" %in% profile_info$packageWhitelist) {
    cli_alert_warning(sprintf("Resourcer not available for profile: %s, skipping testing using resources.", profile))
  } else {
    set_dm_permissions(user = user, admin_pwd = admin_pwd, required_projects = list(project), interactive = interactive, update_auto = update_auto, url = url)

    download_exposome_sources(exposome_ref = exposome_ref, skip_tests = NULL)

    upload_exposome_sources(project = project, exposome_ref = exposome_ref, url = url, token = token, auth_type = auth_type, skip_tests = NULL)

    exposome_resources <- create_exposome_resources(exposome_ref = exposome_ref, project = project, url = url, skip_tests = NULL)

    upload_exposome_resources(project = project, resource = exposome_resources, exposome_ref = exposome_ref)

    assign_exposome_resources(project = project, exposome_ref = exposome_ref)

    resolve_exposome_resources(resource_names = c("description", "exposures", "phenotypes"))

    verify_load_exposome_class()
    verify_exposome_variables()
    verify_exposome_summary_names()
    verify_family_names()
    misssing_summary <- ds.tableMissings("exposome_object", set = "exposures")
    verify_table_missings_names(misssing_summary)
    verify_plot_missings_names(misssing_summary)
    verify_normality_test_names()
    verify_exposure_histogram_names()
    verify_imputation()
    exwas_results <- ds.exwas("blood_pre ~ sex", Set = "exposome_object", family = "gaussian", type = "pooled")
    verify_exwas(exwas_results)
    verify_exposure_cor_dim()
    # verify_exwas_plot(exwas_results) https://github.com/isglobal-brge/dsExposomeClient/issues/19
    # ds.exposome_pca("exposome_object", fam = c("Metals", "Noise")) https://github.com/isglobal-brge/dsExposomeClient/issues/20
    # verify_pca_class() See above
    # verify_pca_plot_class() See above
  }
}
