# install the following packages
install.packages("MolgenisArmadillo")
install.packages("dplyr")
install.packages("arrow")
# After installation make sure you can load the libraries
library(MolgenisArmadillo)
library(dplyr)
library(arrow)
# verify the loaded libraries (see: other attached packages)
sessionInfo()

# optional, install the following packages to be able to run a simple analyses
# 3_Analyse_data_subset_DSMolgenisArmadillo.r
install.packages("DSI")
install.packages("DSMolgenisArmadillo")
install.packages("dsBaseClient", repos = c("http://cran.datashield.org", "https://cloud.r-project.org/"), dependencies = TRUE)

library(dsBaseClient)
library(DSMolgenisArmadillo)
