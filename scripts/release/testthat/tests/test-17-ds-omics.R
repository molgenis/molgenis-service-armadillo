library(dsOmicsClient)
library(purrr)

# Setup
test_name <- "dsOmics"

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
  skip_ds_resource_test(test_name)
  set_dm_permissions()
  upload_many_sources(ref = release_env$omics_ref, folder = "omics")
  omics_resources <- create_many_resources(ref = release_env$omics_ref, folder = "omics")
  upload_many_resources(resource = omics_resources, folder = "omics", ref = release_env$omics_ref)
  assign_many_resources(folder = "omics", ref = release_env$omics_ref)

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
