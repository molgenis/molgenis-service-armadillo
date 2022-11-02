#######
# This assumes you have added a 'test' profile via API or UI
#######

{
  "name": "test",
  "host": "localhost",
  "image": "datashield/armadillo-rserver:6.2.0"
  "port": 6313,
  "whitelist": [
    "dsBase"
  ]
}
#######

install.packages("DSMolgenisArmadillo")
install.packages("remotes")
remotes::install_github("datashield/dsBaseClient", ref="6.2.0")

library(DSMolgenisArmadillo)
library(dsBaseClient)

token <- armadillo.get_token("http://localhost:8080")

builder <- DSI::newDSLoginBuilder()
builder$append(
  server = "local",
  url = "http://localhost:8080",
  token = token,
  driver = "ArmadilloDriver",
  profile = "test"
  )

login_data <- builder$build()

conns <- DSI::datashield.login(logins = login_data, assign = TRUE)