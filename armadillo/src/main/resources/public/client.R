install.packages('remotes')

remotes::install_github('datashield/dsBaseClient', ref = "v6.0-dev")
install.packages(c('DSI','DSMolgenisArmadillo'), repos = 'https://registry.molgenis.org/repository/r-hosted')

library(DSI)
library(DSMolgenisArmadillo)
library(dsBaseClient)

# create loginframe
server <- c("molgenis")
url <- c("https://armadillo.dev.molgenis.org?workspace=GECKO/patient")
user <- c("admin")
password <- c("admin")
driver <- c("ArmadilloDriver")
table <- c("datashield.PATIENT")
logindata <- data.frame(server,url,user,password,table,driver)

# login into datashield service
molgenises <- datashield.login(logins=logindata,assign=F)

# assign some data in R
datashield.assign.table(conns = molgenises, table = "datashield.PATIENT", symbol = "D")

# Do some statistical analysis, execute:
ds.mean('D$age')
