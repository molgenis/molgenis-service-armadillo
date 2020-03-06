install.packages(c('DSI','DSMolgenis'), repos = 'https://registry.molgenis.org/repository/r-hosted')

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
datashield.assign.table(conns = molgenises, table = "datashield.PATIENT", symbol = "datashield.PATIENT")

# Do some statistical analysis, execute:
ds.mean('datashield.PATIENT$age')
