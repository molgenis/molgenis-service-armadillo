install.packages("DSI")
install.packages("DSMolgenisArmadillo")
install.packages("dsBaseClient", repos = c("http://cran.datashield.org", "https://cloud.r-project.org/"), dependencies = TRUE)

library(dsBaseClient)
library(DSMolgenisArmadillo)

# specify server url
armadillo_url <- "https://armadillo.test.molgenis.org"

# get token from central authentication server
token <- armadillo.get_token(armadillo_url)


# build the login dataframe
builder <- DSI::newDSLoginBuilder()
builder$append(
  server = "armadillo",
  url = armadillo_url,
  token = token,
  table = "workshop1/2_1-core-1_0/nonrep",
  driver = "ArmadilloDriver"
)

# create loginframe
logindata <- builder$build()

# login into server
conns <- datashield.login(
  logins = logindata, 
  symbol = "core_nonrep", 
  variables = c("coh_country"), 
  assign = TRUE
)

# calculate the mean
ds.mean("core_nonrep$coh_country", datasources = conns)

ds.histogram(x = "core_nonrep$coh_country", datasources = conns)
