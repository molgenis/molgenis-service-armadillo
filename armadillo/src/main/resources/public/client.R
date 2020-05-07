install.packages('devtools')
library(devtools)

install_github('datashield/dsBaseClient', ref = "v6.0-dev")
install.packages(c('DSI','DSMolgenisArmadillo'), repos = 'https://registry.molgenis.org/repository/r-hosted')

library(DSI)
library(DSMolgenis)
library(dsBaseClient)

# create loginframe
server <- c("molgenis")
url <- c("https://datashield.dev.molgenis.org")
user <- c("admin")
password <- c("admin")
driver <- c("MolgenisDriver")
table <- c("datashield.PATIENT")
logindata <- data.frame(server,url,user,password,table,driver)

# login into datashield service
molgenises <- datashield.login(logins=logindata,assign=F)

# assign some data in R
datashield.assign.table(conns = molgenises, table = "datashield.PATIENT", symbol = "D")

# Do some statistical analysis, execute:
ds.mean('D$age')
