# test-35-ds-omics.R - dsOmics package tests
#
# These tests verify that dsOmics functions work correctly.

# Setup: ensure researcher connection is established (resources mode - no table download)
ensure_researcher_login()
ensure_resources_uploaded()

# Skip all tests if ds-omics is excluded
skip_if_excluded("ds-omics")

# Load the omics client library
library(dsOmicsClient)

# Skip if in admin mode or resourcer not available
config <- config
if (config$ADMIN_MODE) {
  skip("Cannot test omics with basic authentication")
}
if (!"resourcer" %in% test_env$profile_info$packageWhitelist) {
  skip(sprintf("Resourcer not available for profile: %s", config$profile))
}

# Define omics reference data
omics_ref <- tibble::tribble(
  ~file_name, ~path, ~url, ~object_name, ~format,
  "chr1.gds", file.path(config$test_file_path, "chr1.gds"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr1_maf_filtered_small.vcf.gz?raw=true", "chr1", "VCF2GDS",
  "chr2.gds", file.path(config$test_file_path, "chr2.gds"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr2_maf_filtered_small.vcf.gz?raw=true", "chr2", "VCF2GDS",
  "ega_phenotypes.tsv", file.path(config$test_file_path, "ega_phenotypes.tsv"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/ega_synthetic_data_phenotypes_treated_with_nas.tsv?raw=true", "ega_phenotypes", "tsv"
)

# Setup omics data once for all tests
setup_omics_data <- function() {
  if (isTRUE(test_env$omics_setup)) {
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
  download_many_sources(ref = omics_ref)

  # Upload resources
  upload_many_sources(
    project = project,
    ref = omics_ref,
    url = config$armadillo_url,
    folder = "omics",
    token = test_env$token,
    auth_type = config$auth_type
  )

  # Create resources
  omics_resources <- create_many_resources(
    ref = omics_ref,
    project = project,
    folder = "omics",
    url = config$armadillo_url
  )

  # Upload resources to Armadillo
  upload_many_resources(
    project = project,
    folder = "omics",
    resource = omics_resources,
    ref = omics_ref
  )

  # Assign resources
  assign_many_resources(
    project = project,
    folder = "omics",
    ref = omics_ref,
    conns = conns
  )

  # Resolve GDS resources as objects
  purrr::map(c("chr1", "chr2"), function(x) {
    DSI::datashield.assign.expr(
      conns = conns,
      symbol = x,
      expr = as.symbol(paste0("as.resource.object(", x, ")"))
    )
  })

  # Resolve phenotype as data frame
  DSI::datashield.assign.expr(
    conns = conns,
    symbol = "pheno_object",
    expr = quote(as.resource.data.frame(ega_phenotypes))
  )

  test_env$omics_setup <- TRUE
  invisible(TRUE)
}

# Helper to prepare GWAS data
gwas_prepare_data <- function() {
  lapply(1:2, function(x) {
    ds.GenotypeData(
      x = paste0("chr", x),
      covars = "pheno_object",
      columnId = "subject_id",
      sexId = "sex",
      male_encoding = "male",
      female_encoding = "female",
      case_control_column = "diabetes_diagnosed_doctor",
      case = "Yes",
      control = "No",
      newobj.name = paste0("gds.Data", x),
      datasources = conns
    )
  })
}

test_that("omics data can be setup", {
  expect_no_error(setup_omics_data())
  expect_true(test_env$omics_setup)
})

test_that("GWAS data can be prepared", {
  setup_omics_data()

  expect_no_error(gwas_prepare_data())

  # Verify GenotypeData objects were created
  gds1_class <- dsBaseClient::ds.class("gds.Data1", datasources = conns)
  gds2_class <- dsBaseClient::ds.class("gds.Data2", datasources = conns)

  expect_true("GenotypeData" %in% gds1_class$armadillo)
  expect_true("GenotypeData" %in% gds2_class$armadillo)
})

test_that("ds.metaGWAS returns expected dimensions", {
  setup_omics_data()

  # Ensure GWAS data is prepared
  tryCatch({
    dsBaseClient::ds.class("gds.Data1")
  }, error = function(e) {
    gwas_prepare_data()
  })

  # Run meta GWAS
  gwas_results <- ds.metaGWAS(
    genoData = paste0("gds.Data", 1:2),
    model = diabetes_diagnosed_doctor ~ sex + hdl_cholesterol
  )[[1]]

  gwas_dim <- dim(gwas_results)

  expect_identical(gwas_dim, as.integer(c(144785, 10)))
})
