library(dsOmicsClient)

# Setup
test_name <- "ds-omics"
release_env$omics_resolved <- c(chr1 = FALSE, chr2 = FALSE, ega_phenotypes = FALSE)

skip_if_omics_not_resolved <- function() {
  skip_if(!all(release_env$omics_resolved), "Error resolving omics resources")
}

gwas_prepare_data <- function() {
  ds.GenotypeData(
    x = "chr1", covars = "pheno_object",
    columnId = "subject_id", sexId = "sex",
    male_encoding = "male", female_encoding = "female",
    newobj.name = "gds.Data1", datasources = release_env$conns
  )
}

# Assign resources individually
test_that("assign chr1 resource", {
  skip_if_no_resources(test_name)
  datashield.assign.resource(release_env$conns,
    resource = paste0(release_env$project1, "/omics/chr1"), symbol = "chr1")
  resource_class <- ds.class("chr1", datasources = release_env$conns)
  expected <- c("GDSFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
  expect_identical(resource_class$armadillo, expected)
})

test_that("assign chr2 resource", {
  skip_if_no_resources(test_name)
  datashield.assign.resource(release_env$conns,
    resource = paste0(release_env$project1, "/omics/chr2"), symbol = "chr2")
  resource_class <- ds.class("chr2", datasources = release_env$conns)
  expected <- c("GDSFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
  expect_identical(resource_class$armadillo, expected)
})

test_that("assign ega_phenotypes resource", {
  skip_if_no_resources(test_name)
  datashield.assign.resource(release_env$conns,
    resource = paste0(release_env$project1, "/omics/ega_phenotypes"), symbol = "ega_phenotypes")
  resource_class <- ds.class("ega_phenotypes", datasources = release_env$conns)
  expected <- c("TidyFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
  expect_identical(resource_class$armadillo, expected)
})

# Resolve resources individually
test_that("resolve chr1 resource", {
  skip_if_no_resources(test_name)
  DSI::datashield.assign.expr(release_env$conns, symbol = "chr1",
    expr = as.symbol("as.resource.object(chr1)"))
  resource_class <- ds.class("chr1", datasources = release_env$conns)
  expect_identical(as.character(resource_class$armadillo), "GdsGenotypeReader")
  release_env$omics_resolved[["chr1"]] <- TRUE
})

test_that("resolve chr2 resource", {
  skip_if_no_resources(test_name)
  DSI::datashield.assign.expr(release_env$conns, symbol = "chr2",
    expr = as.symbol("as.resource.object(chr2)"))
  resource_class <- ds.class("chr2", datasources = release_env$conns)
  expect_identical(as.character(resource_class$armadillo), "GdsGenotypeReader")
  release_env$omics_resolved[["chr2"]] <- TRUE
})

test_that("resolve ega_phenotypes resource", {
  skip_if_no_resources(test_name)
  DSI::datashield.assign.expr(release_env$conns, symbol = "pheno_object",
    expr = quote(as.resource.data.frame(ega_phenotypes)))
  resource_class <- ds.class("pheno_object", datasources = release_env$conns)
  dims <- ds.dim("pheno_object", datasources = release_env$conns)[[1]]
  expect_identical(resource_class$armadillo, c("spec_tbl_df", "tbl_df", "tbl", "data.frame"))
  expect_identical(dims, as.integer(c(2504, 74)))
  if (identical(dims, as.integer(c(2504, 74)))) release_env$omics_resolved[["ega_phenotypes"]] <- TRUE
})

# Function tests
test_that("ds.genoDimensions", {
  skip_if_no_resources(test_name)
  skip_if_omics_not_resolved()
  dims <- ds.genoDimensions("chr1", datasources = release_env$conns)
  expect_identical(dims$armadillo$snp_number, 69806L)
  expect_identical(dims$armadillo$scan_number, 2504L)
  expect_identical(dims$armadillo$chromosomes, "1")
})

test_that("ds.alleleFrequency", {
  skip_if_no_resources(test_name)
  skip_if_omics_not_resolved()
  gwas_prepare_data()
  freqs <- ds.alleleFrequency("gds.Data1", datasources = release_env$conns)
  expect_identical(class(freqs), c("dsalleleFrequency", "tbl_df", "tbl", "data.frame"))
  expect_identical(dim(freqs), c(69762L, 3L))
  expect_identical(colnames(freqs), c("rs", "n", "pooled_MAF"))
})
