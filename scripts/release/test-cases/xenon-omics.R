devtools::install_github('isglobal-brge/dsOmicsClient')

library("dsOmicsClient")
source("test-cases/download-resources.R")
library(purrr)

demo_url <- "https://armadillo-demo.molgenis.net/"
demo_token <- armadillo.get_token(demo_url)

builder <- DSI::newDSLoginBuilder()

builder$append(
  server = "armadillo",
  url = demo_url,
  profile = "xenon",
  driver = "ArmadilloDriver",
  token = demo_token
)

logindata <- builder$build()
conns <- DSI::datashield.login(logins = logindata, assign = F)


# 
# 
# set_dm_permissions(user = user, admin_pwd = admin_pwd, required_projects = list(project), interactive = interactive, update_auto = update_auto, url = url)
# 
omics_ref <- tribble(
  ~file_name, ~path, ~url, ~object_name, ~format,
  "chr1.gds", file.path(test_file_path, "chr1.gds"), "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr1_maf_filtered_small.vcf.gz?raw=true", "chr1", "VCF2GDS",
  "ega_phenotypes.tsv", file.path(test_file_path, "ega_phenotypes.tsv"), "https://opal-demo.obiba.org/ui/index.html#!project;name=GWAS;tab=RESOURCES;path=GWAS.ega_phenotypes:~:text=URL-,https%3A//github.com/isglobal%2Dbrge/brge_data_large/blob/master/inst/extdata/GWAS_example/ega_synthetic_data_phenotypes_treated_with_nas.tsv%3Fraw%3Dtrue,-Format", "ega_phenotypes", "tsv"
)

download_many_sources(exposome_ref = omics_ref, skip_tests = NULL)

upload_many_sources(project = project, exposome_ref = omics_ref, url = url, token = token, auth_type = auth_type, skip_tests = NULL)

exposome_resources <- create_many_resources(exposome_ref = exposome_ref, project = project, url = url, skip_tests = NULL)

upload_many_resources(project = project, resource = exposome_resources, exposome_ref = exposome_ref)

assign_many_resources(project = project, exposome_ref = exposome_ref)












####################################################################################################
# Get resources  
####################################################################################################

## ---- GWAS ---------------------------------------------------------------------------------------
gwas_arm_path <- "/Users/tcadman/Library/Mobile Documents/com~apple~CloudDocs/work/repos/molgenis-service-armadillo/data/testing/chr1.gds"
gwas_source_path <- "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr1_maf_filtered_small.vcf.gz?raw=true"

prepare_resources(resource_path = gwas_arm_path, url = gwas_source_path, skip_tests = NULL)

upload_resource(
  project = "ybsya5rgb4", rda_dir = gwas_arm_path, url = test_config$armadillo_url, 
  token = demo_token, folder = "omics", file_name = "chr1.gds", 
  auth_type = test_config$auth_type, skip_tests = NULL)

omics_resources <- create_resource(
  target_project = "ybsya5rgb4", url = test_config$armadillo_url, 
  folder = "omics", file_name = "chr1.gds", resource_name = "chr1", 
  format = "VCF2GDS", skip_tests = NULL)

armadillo.upload_resource(project = "ybsya5rgb4", folder = "omics", resource = omics_resources, name = "chr1")
# armadillo.delete_resource(project = "ybsya5rgb4", folder = "omics", name = "ega_phenotypes")

## ---- EGA phenotypes -----------------------------------------------------------------------------
ega_arm_path <- "/Users/tcadman/Library/Mobile Documents/com~apple~CloudDocs/work/repos/molgenis-service-armadillo/data/testing/ega_phenotypes"
ega_source_path <- "https://opal-demo.obiba.org/ui/index.html#!project;name=GWAS;tab=RESOURCES;path=GWAS.ega_phenotypes:~:text=URL-,https%3A//github.com/isglobal%2Dbrge/brge_data_large/blob/master/inst/extdata/GWAS_example/ega_synthetic_data_phenotypes_treated_with_nas.tsv%3Fraw%3Dtrue,-Format"

prepare_resources(resource_path = ega_arm_path, url = ega_source_path, skip_tests = NULL)

upload_resource(
  project = "ybsya5rgb4", rda_dir = ega_arm_path, url = test_config$armadillo_url, 
  token = demo_token, folder = "omics", file_name = "ega_phenotypes.tsv", 
  auth_type = test_config$auth_type, skip_tests = NULL)

ega_resources <- create_resource(
  target_project = "ybsya5rgb4", url = test_config$armadillo_url, 
  folder = "omics", file_name = "ega_phenotypes.tsv", resource_name = "ega_phenotypes", 
  format = "tsv", skip_tests = NULL)

armadillo.upload_resource(project = "ybsya5rgb4", folder = "omics", resource = ega_resources, name = "ega_phenotypes")






################################################################################
# GWAS test  
################################################################################
gwas_assign_resources <- function() {
  
  lapply(1:2, function(x){
    DSI::datashield.assign.resource(conns, paste0("chr", x), paste0("GWAS.chr", x))
  })
  
  lapply(1:2, function(x){
    DSI::datashield.assign.expr(conns = conns, symbol = paste0("gds", x, "_object"),
                                expr = as.symbol(paste0("as.resource.object(chr", x, ")")))
  })
  
  DSI::datashield.assign.resource(conns, "pheno", "GWAS.ega_phenotypes")
  DSI::datashield.assign.expr(conns = conns, symbol = "pheno_object",
                              expr = quote(as.resource.data.frame(pheno)))
  
}

gwas_prepare_data <- function() {
  
  lapply(1:2, function(x){
    ds.GenotypeData(x=paste0('gds', x,'_object'), covars = 'pheno_object', 
                    columnId = "subject_id", sexId = "sex", 
                    male_encoding = "male", female_encoding = "female",
                    case_control_column = "diabetes_diagnosed_doctor", 
                    case = "Yes", control = "No", 
                    newobj.name = paste0('gds.Data', x), datasources = conns)
  })
  
}

gwas_assign_resources()
gwas_prepare_data()

verify_meta_gwas <- function() {
  gwas_results <- ds.metaGWAS(genoData = paste0("gds.Data", 1:3), 
                              model = diabetes_diagnosed_doctor ~ sex + hdl_cholesterol)[[1]]
  
  gwas_dim <- dim(gwas_results)
  
  verify_output(function_name = "ds.metaGWAS", object = gwas_dim, 
                expected = as.integer(c(197945, 10)), 
                fail_msg = xenon_fail_msg$clt_dim)
  
}

verify_meta_gwas()





