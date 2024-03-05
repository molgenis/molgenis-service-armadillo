library(dsExposomeClient)
library(purrr)

assign_exposome_resources <- function(resource_name) {
  exp_resource_path <- paste0("xenon-tests/exposome/", resource_name)
  datashield.assign.resource(conns, resource = exp_resource_path, symbol = resource_name)
}

resolve_exposome_resources <- function(resource_name) {
  datashield.assign.expr(conns, symbol = resource_name, expr = as.symbol(paste0("as.resource.data.frame(", resource_name, ")")))
}

verify_load_exposome_class <- function() {
  ds_function_name <- "ds.loadExposome"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.loadExposome(exposures = "exposures", phenotypes = "phenotypes", exposures.idcol = "idnum",
                  phenotypes.idcol = "idnum", description = "description", description.expCol = "Exposure",
                  description.famCol = "Family", object_name = "exposome_object")
  obj_class <- ds.class("exposome_object")
  verify_output(function_name = ds_function_name, object = as.character(obj_class$armadillo),
                expected = "ExposomeSet", fail_msg = xenon_fail_msg$srv_class)
}

verify_exposome_variables <- function() {
  ds_function_name <- "ds.exposome_variables"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  vars <- ds.exposome_variables("exposome_object", "phenotypes")
  verify_output(function_name = ds_function_name, object = vars$armadillo,
                expected = c("whistling_chest", "flu", "rhinitis", "wheezing", "birthdate", "sex", "age", "cbmi", "blood_pre"),
                fail_msg = xenon_fail_msg$clt_vars)
}

verify_exposome_summary_names <- function() {
  ds_function_name <- "ds.exposome_summary"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  var_summary <- ds.exposome_summary("exposome_object", "AbsPM25")
  verify_output(function_name = ds_function_name, object = names(var_summary$armadillo),
                expected = c("class", "length","quantiles & mean"),
                fail_msg = xenon_fail_msg$clt_list_names)
}

verify_family_names <- function() {
  ds_function_name <- "ds.familyNames"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  vars <- ds.familyNames("exposome_object")
  verify_output(function_name = ds_function_name, object = vars$armadillo,
                expected = c("Air Pollutants", "Metals", "PBDEs", "Organochlorines", "Bisphenol A", "Water Pollutants",
                 "Built Environment", "Cotinine", "Home Environment", "Phthalates", "Noise", "PFOAs", "Temperature"),
                fail_msg = xenon_fail_msg$clt_list_names)

}

verify_table_missings_names <- function(misssing_summary) {
  ds_function_name <- "ds.tableMissings"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  verify_output(function_name = ds_function_name, object = names(misssing_summary),
                expected = c("pooled", "set","output"), fail_msg = list_names_msg)

}

verify_plot_missings_names <- function(misssing_summary) {
  ds_function_name <- "ds.plotMissings"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  missing_plot <- ds.plotMissings(misssing_summary)
  verify_output(function_name = ds_function_name, object = names(missing_plot$pooled),
                expected = c("data", "layers", "scales", "guides", "mapping", "theme", "coordinates",
                             "facet", "plot_env", "layout", "labels"),
                fail_msg = xenon_fail_msg$clt_list_names)
}

verify_normality_test_names <- function() {
  ds_function_name <- "ds.normalityTest"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  nm <- ds.normalityTest("exposome_object")
  verify_output(function_name = ds_function_name, object = names(nm$armadillo),
                expected = c("exposure", "normality", "p.value"),
                fail_msg = xenon_fail_msg$clt_list_names)
}

verify_exposure_histogram_names <- function() {
  ds_function_name <- "ds.exposure_histogram"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  hist <- ds.exposure_histogram("exposome_object", "AbsPM25")
  verify_output(function_name = ds_function_name, object = names(hist),
                expected = c("breaks", "counts", "density", "mids", "xname", "equidist"),
                fail_msg = xenon_fail_msg$clt_list_names)

}

verify_imputation <- function() {
  ds_function_name <- "ds.imputation"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.imputation("exposome_object", "exposome_object_imputed")
  obj_class <- ds.class("exposome_object_imputed")
  verify_output(function_name = ds_function_name, object = as.character(obj_class$armadillo),
                expected = "ExposomeSet", fail_msg = xenon_fail_msg$srv_class)
}

verify_exwas <- function(exwas_results) {
  ds_function_name <- "ds.exwas"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  verify_output(function_name = ds_function_name, object = class(exwas_results),
                expected = c("list", "dsExWAS_pooled"), fail_msg = xenon_fail_msg$clt_class)
}

verify_exwas_plot <- function(exwas_results) {
  ds_function_name <- "ds.exwas"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  exwas_plot <- ds.plotExwas(exwas_results, type="effect")
  verify_output(function_name = ds_function_name, object = class(exwas_plot),
                expected = c("gg", "ggplot"), fail_msg = xenon_fail_msg$clt_class)
}

verify_pca_class <- function(ds_function_name) {
  ds_function_name <- "ds.exposome_pca"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  ds.exposome_pca("exposome_object", fam = c("Metals", "Noise"))
  pca_class <- ds.class("ds.exposome_pca.Results")
  verify_output(function_name = ds_function_name, object = as.character(pca_class),
                expected = "ExposomePCA", fail_msg = xenon_fail_msg$clt_class)

}

verify_pca_plot_class <- function(ds_function_name) {
  ds_function_name <- "ds.exposome_pca_plot"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  pca_plot <- ds.exposome_pca_plot("ds.exposome_pca.Results", set = "all", method = 1, k = 3, noise = 5)
  verify_output(function_name = ds_function_name, object = class(pca_plot),
                expected = c("gtable", "gTree", "grob", "gDesc"), fail_msg = xenon_fail_msg$clt_class)
}

verify_exposure_cor_dim <- function(ds_function_name) {
  ds_function_name <- "ds.exposome_correlation"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  exposome_cor <- ds.exposome_correlation("exposome_object", c("Metals", "Noise"))[[1]][[1]]$`Correlation Matrix`[1:5,1:5]
  verify_output(function_name = ds_function_name, object = dim(exposome_cor),
                expected = as.integer(c(5, 5)), fail_msg = xenon_fail_msg$clt_dim)
}


run_exposome_tests <- function(project, conns) {
  map(c("description", "exposures", "phenotypes", "exposomeSet"), assign_exposome_resources)
  map(c("description", "exposures", "phenotypes"), resolve_exposome_resources)
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
  exwas_results <- ds.exwas("blood_pre ~ sex", Set = "exposome_object", family = "gaussian")
  verify_exwas(exwas_results)
  verify_exwas_plot(exwas_results)
  verify_pca_class()
  verify_pca_plot_class()
  verify_exposure_cor_dim()
}

run_exposome_tests()
