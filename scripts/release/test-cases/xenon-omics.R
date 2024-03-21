library(dsOmicsClient)
library(purrr)
gwas_prepare_data <- function() {
  lapply(1:2, function(x) {
    ds.GenotypeData(
      x = paste0("chr", x), covars = "pheno_object",
      columnId = "subject_id", sexId = "sex",
      male_encoding = "male", female_encoding = "female",
      case_control_column = "diabetes_diagnosed_doctor",
      case = "Yes", control = "No",
      newobj.name = paste0("gds.Data", x), datasources = conns
    )
  })
}

verify_meta_gwas <- function(gwas_results) {
  gwas_dim <- dim(gwas_results)
  verify_output(
    function_name = "ds.metaGWAS", object = gwas_dim,
    expected = as.integer(c(139608, 10)),
    fail_msg = xenon_fail_msg$clt_dim
  )
}

omics_ref <- tribble(
  ~file_name, ~path, ~url, ~object_name, ~format,
  "chr1.gds", file.path(test_config$test_file_path, "chr1.gds"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr1_maf_filtered_small.vcf.gz?raw=true", "chr1", "VCF2GDS",
  "chr2.gds", file.path(test_config$test_file_path, "chr2.gds"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr2_maf_filtered_small.vcf.gz?raw=true", "chr2", "VCF2GDS",
  "ega_phenotypes.tsv", file.path(test_config$test_file_path, "ega_phenotypes.tsv"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/ega_synthetic_data_phenotypes_treated_with_nas.tsv?raw=true", "ega_phenotypes", "tsv"
)

run_omics_tests <- function(project, url, token, auth_type, ADMIN_MODE, profile, profile_info, ref,
                            skip_tests, user, admin_pwd, interactive, update_auto) {
  test_name <- "xenon-omics"
  if (do_skip_test(test_name, skip_tests)) {
    return()
  }
  if (ADMIN_MODE) {
    cli_alert_warning("Cannot test working with resources as basic authenticated admin")
  } else if (!"resourcer" %in% profile_info$packageWhitelist) {
    cli_alert_warning(sprintf("Resourcer not available for profile: %s, skipping testing using resources.", profile))
  } else {
    set_dm_permissions(
      user = user, admin_pwd = admin_pwd, required_projects = list(project),
      interactive = interactive, update_auto = update_auto, url = url
    )

    download_many_sources(ref = omics_ref, skip_tests = NULL)

    upload_many_sources(
      project = project, ref = omics_ref, folder = "omics", url = url,
      token = token, auth_type = auth_type, skip_tests = NULL
    )

    omics_resources <- create_many_resources(
      ref = omics_ref, project = project,
      folder = "omics", url = url, skip_tests = NULL
    )

    upload_many_resources(
      project = project, folder = "omics", resource = omics_resources,
      ref = omics_ref
    )

    assign_many_resources(project = project, folder = "omics", ref = omics_ref)

    map(c("chr1", "chr2"), ~ DSI::datashield.assign.expr(
      conns = conns, symbol = .x, expr = as.symbol(paste0("as.resource.object(", .x, ")"))
    ))

    DSI::datashield.assign.expr(conns = conns, symbol = "pheno_object", expr = quote(as.resource.data.frame(ega_phenotypes)))

    gwas_prepare_data()
    gwas_results <- ds.metaGWAS(
      genoData = paste0("gds.Data", 1:2),
      model = diabetes_diagnosed_doctor ~ sex + hdl_cholesterol
    )[[1]]
    verify_meta_gwas(gwas_results)
  }
  cli_alert_success(sprintf("%s passed!", test_name))
}
