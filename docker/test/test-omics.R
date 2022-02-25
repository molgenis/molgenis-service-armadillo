# https://github.com/obiba/resourcer#file-data-format
# https://htmlpreview.github.io/?https://github.com/isglobal-brge/dsOmicsClient/blob/master/vignettes/dsOmics.html
#   Illustrative example: Epigenome-wide association analysis (EWAS)
# https://opal-demo.obiba.org/
#   username: administrator
#   password: password

install.packages(c("resourcer", "MolgenisArmadillo"), dependencies=TRUE)

# make a EWAS file resource
resGSE1 <- resourcer::newResource(
  name = "GSE66351_1",
  url = "https://github.com/isglobal-brge/brge_data_large/blob/master/data/gse66351_1.rda?raw=true",
  format = "ExpressionSet"
)
resGSE2 <- resourcer::newResource(
  name = "GSE66351_2",
  url = "https://github.com/isglobal-brge/brge_data_large/blob/master/data/gse66351_2.rda?raw=true",
  format = "ExpressionSet"
)

MolgenisArmadillo::armadillo.login(armadillo = "http://localhost:8080", minio = "http://localhost:9000")
MolgenisArmadillo::armadillo.create_project("omics")
MolgenisArmadillo::armadillo.upload_resource(project="omics", folder="ewas", resource = resGSE1, name = "GSE66351_1")
MolgenisArmadillo::armadillo.upload_resource(project="omics", folder="ewas", resource = resGSE2, name = "GSE66351_2")

install.packages(c("remotes", "DSMolgenisArmadillo", "DSI"))
library(remotes)
install_github("isglobal-brge/dsOmicsClient")

library(DSMolgenisArmadillo)
library(DSI)
library(dsBaseClient) 
library(dsOmicsClient)

builder <- DSI::newDSLoginBuilder()
builder$append(server = "study1", url = "http://localhost:8080",
               user = "admin", password = "admin",
               resource = "omics/ewas/GSE66351_1",
               driver="ArmadilloDriver",
               profile="omics")
builder$append(server = "study2", url = "http://localhost:8080",
               user = "admin", password = "admin",
               resource = "omics/ewas/GSE66351_2",
               driver="ArmadilloDriver",
               profile="omics")
logindata <- builder$build()

conns <- DSI::datashield.login(logins = logindata, assign = TRUE,
                               symbol = "res")

datashield.assign.expr(conns, symbol = "methyl",
                       expr = quote(as.resource.object(res)))

ds.class("methyl")

fn <- ds.featureNames("methyl")
lapply(fn, head)

ans <- ds.lmFeature(feature = "cg07363416", 
                    model = ~ diagnosis + Sex, 
                    Set = "methyl",
                    datasources = conns)
ans

datashield.logout(conns)
