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
  url = "https://github.com/isglobal-brge/brgedata/raw/master/data/gse66351_1.rda",
  format = "ExpressionSet"
)
resGSE2 <- resourcer::newResource(
  name = "GSE66351_2",
  url = "https://github.com/isglobal-brge/brgedata/raw/master/data/gse66351_2.rda",
  format = "ExpressionSet"
)

# coerce the csv file in the opal server to a data.frame
resourceGSE1 <- as.data.frame(resGSE1)
resourceGSE2 <- as.data.frame(resGSE2)

MolgenisArmadillo::armadillo.login(armadillo = "localhost:8080", minio = "localhost:9000")
MolgenisArmadillo::armadillo.create_project("omics")
MolgenisArmadillo::armadillo.upload_resource(project="omics", folder="ewas", resource = resGSE1, name = "GSE66351_1")
MolgenisArmadillo::armadillo.upload_resource(project="omics", folder="ewas", resource = resGSE2, name = "GSE66351_2")

install.packages(c("DSMolgenisArmadillo", "DSI"))

builder <- DSI::newDSLoginBuilder()
builder$append(server = "study1", url = "http://localhost:8080",
               user = "admin", password = "admin",
               resource = "omics/ewas/GSE66351_1")
builder$append(server = "study2", url = "http://localhost:8080",
               user = "admin", password = "admin",
               resource = "omics/ewas/GSE66351_2")


logindata <- builder$build()

conns <- DSI::datashield.login(logins = logindata, assign = TRUE,
                               symbol = "res")

datashield.assign.expr(conns, symbol = "methyl",
                       expr = quote(as.resource.object(res)))

ds.class("methyl")

fn <- ds.featureNames("methyl")
lapply(fn, head)
