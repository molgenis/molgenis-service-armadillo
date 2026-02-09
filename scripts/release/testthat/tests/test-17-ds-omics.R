library(dsOmicsClient)
library(purrr)
library(tibble)

# Load helper functions (paths relative to release directory)
source("../../lib/upload-resource.R")
source("../../lib/create-resource.R")
source("../../lib/download-resources.R")

# Setup
test_name <- "ds-omics"

omics_ref <- tribble(
  ~file_name, ~path, ~url, ~object_name, ~format,
  "chr1.gds", file.path(release_env$test_file_path, "chr1.gds"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr1_maf_filtered_small.vcf.gz?raw=true", "chr1", "VCF2GDS",
  "chr2.gds", file.path(release_env$test_file_path, "chr2.gds"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr2_maf_filtered_small.vcf.gz?raw=true", "chr2", "VCF2GDS",
  "ega_phenotypes.tsv", file.path(release_env$test_file_path, "ega_phenotypes.tsv"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/ega_synthetic_data_phenotypes_treated_with_nas.tsv?raw=true", "ega_phenotypes", "tsv"
)

gwas_prepare_data <- function() {
  lapply(1:2, function(x) {
    ds.GenotypeData(
      x = paste0("chr", x), covars = "pheno_object",
      columnId = "subject_id", sexId = "sex",
      male_encoding = "male", female_encoding = "female",
      case_control_column = "diabetes_diagnosed_doctor",
      case = "Yes", control = "No",
      newobj.name = paste0("gds.Data", x), datasources = release_env$conns
    )
  })
}

test_that("ds.metaGWAS", {
  do_skip_test(test_name)
  skip_if(release_env$ADMIN_MODE, "Cannot test resources as admin")
  skip_if(!"resourcer" %in% release_env$profile_info$packageWhitelist,
          sprintf("resourcer not available for profile: %s", release_env$current_profile))

  set_dm_permissions()
  download_many_sources(ref = omics_ref)
  upload_many_sources(ref = omics_ref, folder = "omics")
  omics_resources <- create_many_resources(ref = omics_ref, folder = "omics")
  upload_many_resources(resource = omics_resources, folder = "omics", ref = omics_ref)
  assign_many_resources(folder = "omics", ref = omics_ref)

  map(c("chr1", "chr2"), ~ DSI::datashield.assign.expr(
    conns = release_env$conns, symbol = .x, expr = as.symbol(paste0("as.resource.object(", .x, ")"))
  ))

  DSI::datashield.assign.expr(conns = release_env$conns, symbol = "pheno_object", expr = quote(as.resource.data.frame(ega_phenotypes)))

  gwas_prepare_data()
  gwas_results <- ds.metaGWAS(
    genoData = paste0("gds.Data", 1:2),
    model = diabetes_diagnosed_doctor ~ sex + hdl_cholesterol,
    datasources = release_env$conns
  )[[1]]

  gwas_dim <- dim(gwas_results)
  expect_identical(gwas_dim, as.integer(c(144785, 10)))
})
