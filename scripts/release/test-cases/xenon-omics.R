devtools::install_github('isglobal-brge/dsOmicsClient')

library("dsOmicsClient")
source("test-cases/download-resources.R")

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

gwas_path <- "/Users/tcadman/Library/Mobile Documents/com~apple~CloudDocs/work/repos/molgenis-service-armadillo/data/testing/chr1.gds"
gwas_1 <- "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr1_maf_filtered_small.vcf.gz?raw=true"


set_dm_permissions(user = user, admin_pwd = admin_pwd, required_projects = list(project), interactive = interactive, update_auto = update_auto, url = url)

prepare_resources(resource_path = gwas_path, url = gwas_1, skip_tests = NULL)
upload_resource(project = "ybsya5rgb4", rda_dir = gwas_path, url = test_config$armadillo_url, token = demo_token, folder = "omics", file_name = "chr1.gds", auth_type = test_config$auth_type, skip_tests = NULL)

test_chr <- create_resource(target_project = "ybsya5rgb4", url = test_config$armadillo_url, folder = "omics", file_name = "chr1.gds", resource_name = "chr1", format = "VCF2GDS", skip_tests = NULL)

armadillo.upload_resource(project = "ybsya5rgb4", folder = "omics", resource = test_chr, name = "chr1")

exp_resource_path <- paste0("ybsya5rgb4", "/omics/", "chr1")
datashield.assign.resource(conns, resource = exp_resource_path, symbol = "chr1")

DSI::datashield.assign.expr(conns = conns, symbol = "gds1_object",expr = as.symbol(paste0("as.resource.object(chr1)")))





upload_exposome_sources(project = project, exposome_ref = exposome_ref, url = url, token = token, auth_type = auth_type, skip_tests = NULL)

exposome_resources <- create_exposome_resources(exposome_ref = exposome_ref, project = project, url = url, skip_tests = NULL)

upload_exposome_resources(project = project, resource = exposome_resources, exposome_ref = exposome_ref)

assign_exposome_resources(project = project, exposome_ref = exposome_ref)

resolve_exposome_resources(resource_names = c("description", "exposures", "phenotypes"))

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

################################################################################
# PRS test  
################################################################################
# 
# equire('DSI')
# require('DSOpal')
# require('dsBaseClient')
# require('dsOmicsClient')
# 
# builder <- DSI::newDSLoginBuilder()
# builder$append(server = "cohort1", url = "https://opal-demo.obiba.org/",
#                user =  "dsuser", password = "P@ssw0rd",
#                driver = "OpalDriver", profile = "omics")
# builder$append(server = "cohort2", url = "https://opal-demo.obiba.org/",
#                user =  "dsuser", password = "P@ssw0rd",
#                driver = "OpalDriver", profile = "omics")
# builder$append(server = "cohort3", url = "https://opal-demo.obiba.org/",
#                user =  "dsuser", password = "P@ssw0rd",
#                driver = "OpalDriver", profile = "omics")
# logindata <- builder$build()
# conns <- DSI::datashield.login(logins = logindata)
# 
# # Cohort 1 resources
# lapply(1:21, function(x){
#   DSI::datashield.assign.resource(conns[1], paste0("chr", x), paste0("GWAS.chr", x,"A"))
# })
# DSI::datashield.assign.resource(conns[1], "phenotypes_resource", "GWAS.ega_phenotypes_1")
# 
# # Cohort 2 resources
# lapply(1:21, function(x){
#   DSI::datashield.assign.resource(conns[2], paste0("chr", x), paste0("GWAS.chr", x,"B"))
# })
# DSI::datashield.assign.resource(conns[2], "phenotypes_resource", "GWAS.ega_phenotypes_2")
# 
# # Cohort 3 resources
# lapply(1:21, function(x){
#   DSI::datashield.assign.resource(conns[3], paste0("chr", x), paste0("GWAS.chr", x,"C"))
# })
# DSI::datashield.assign.resource(conns[3], "phenotypes_resource", "GWAS.ega_phenotypes_3")
# 
# DSI::datashield.assign.expr(conns = conns, symbol = "phenotypes",
#                             expr = as.symbol("as.resource.data.frame(phenotypes_resource)"))
# 
# lapply(1:21, function(x){
#   DSI::datashield.assign.expr(conns = conns, symbol = paste0("gds", x, "_object"),
#                               expr = as.symbol(paste0("as.resource.object(chr", x, ")")))
# })
# 
# resources <- paste0("gds", 1:21, "_object")
# 
# # HDL cholesterol
# ds.PRS(resources = resources, pgs_id = "PGS000660", 
#        table = "phenotypes", table_id_column = "subject_id")

################################################################################
# PLINK  
################################################################################
builder <- newDSLoginBuilder()
builder$append(server = "study1", url = "https://opal-demo.obiba.org",
               user = "dsuser", password = "P@ssw0rd",
               resource = "RSRC.brge_plink",
               profile = "omics")
logindata <- builder$build()
conns <- datashield.login(logins = logindata, assign = TRUE,
                          symbol = "client")

verify_plink <- function() {
  ds_function_name <- "ds.PLINK"
  cli_alert_info(sprintf("Checking %s", ds_function_name))
  plink.arguments <- "--bfile brge --logistic --pheno brge.phe --mpheno 6 --covar brge.phe --covar-name gender,age"
  ans.plink <- ds.PLINK("client", plink.arguments)
  verify_output(function_name = ds_function_name, object = dim(ans.plink$study1$results), 
                expected = as.integer(c(302587,10)), fail_msg = xenon_fail_msg$clt_dim)
}

verify_plink()


################################################################################
# SNPTEST  
################################################################################
library(DSOpal)
library(dsBaseClient)
library(dsOmicsClient)

builder <- newDSLoginBuilder()

builder$append(server = "study1", url = "https://opal-demo.obiba.org",
               user = "dsuser", password = "P@ssw0rd",
               resource = "RSRC.brge_snptest",
               profile = "omics")

logindata <- builder$build()

conns <- datashield.login(logins = logindata, assign = TRUE, symbol = "client")

snptest.arguments <- "-frequentist 1 -method score -pheno bin1 -data cohort1.gen cohort1.sample cohort2.gen cohort2.sample"

ans.snptest <- ds.snptest("client", snptest.arguments)






