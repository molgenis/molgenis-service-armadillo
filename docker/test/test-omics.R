# https://github.com/obiba/resourcer#file-data-format

install.packages("resourcer", dependencies=TRUE)
install.packages("MolgenisArmadillo", dependencies=TRUE)

# make a SPSS file resource
res <- resourcer::newResource(
  name = "chr1",
  url = "https://github.com/isglobal-brge/brge_data_large/blob/master/inst/extdata/GWAS_example/chr1_maf_filtered_small.vcf.gz?raw=true",
  format = "VCF2GDS"
)

# coerce the csv file in the opal server to a data.frame
df <- as.data.frame(res)

MolgenisArmadillo::armadillo.login(armadillo = "localhost:8080", minio = "localhost:9000")
MolgenisArmadillo::armadillo.create_project("omics")
MolgenisArmadillo::armadillo.upload_resource(project="omics", folder="omics")
