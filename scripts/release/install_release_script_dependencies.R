#!/usr/bin/env Rscript
cat("


  __  __  ____  _      _____ ______ _   _ _____  _____                                     _ _ _ _
 |  \\/  |/ __ \\| |    / ____|  ____| \\ | |_   _|/ ____|     /\\                            | (_) | |
 | \\  / | |  | | |   | |  __| |__  |  \\| | | | | (___      /  \\   _ __ _ __ ___   __ _  __| |_| | | ___
 | |\\/| | |  | | |   | | |_ |  __| | . ` | | |  \\___ \\    / /\\ \\ | '__| '_ ` _ \\ / _` |/ _` | | | |/ _ \\
 | |  | | |__| | |___| |__| | |____| |\\  |_| |_ ____) |  / ____ \\| |  | | | | | | (_| | (_| | | | | (_) |
 |_|  |_|\\____/|______\\_____|______|_| \\_|_____|_____/  /_/    \\_\\_|  |_| |_| |_|\\__,_|\\__,_|_|_|_|\\___/

  _____      _                       _            _
 |  __ \\    | |                     | |          | |
 | |__) |___| | ___  __ _ ___  ___  | |_ ___  ___| |_
 |  _  // _ \\ |/ _ \\/ _` / __|/ _ \\ | __/ _ \\/ __| __|
 | | \\ \\  __/ |  __/ (_| \\__ \\  __/ | ||  __/\\__ \\ |_
 |_|  \\_\\___|_|\\___|\\__,_|___/\\___|  \\__\\___||___/\\__|


")
cat("Installing dependencies for release test\n")
if(!"cli" %in% installed.packages()) {
  install.packages("cli", repos = "http://cran.us.r-project.org")
}
library(cli)
cli_alert_info("Installing packages")

packages <- c("diffobj", "getPass", "arrow", "jsonlite", "future",
  "RCurl",
  "devtools",
  "DSI",
  "resourcer",
  "MolgenisArmadillo",
  "DSMolgenisArmadillo",
  "purrr"
)

install_requirements_from_cran <- function(packages) {
  n_requirements <- length(packages)
  cli_progress_bar("Installing packages", total = n_requirements)
  for (i in 1:n_requirements) {
    pkg <- packages[i]
    if (pkg %in% installed.packages()){
      cli_alert_info(sprintf("Package [%s] already installed, skipping.", pkg))
    } else{
      cli_alert_info(paste0("Installing ", pkg))
      install.packages(pkg, repos = "http://cran.us.r-project.org", quiet=TRUE)
    }
    cli_progress_update()
  }
  cli_progress_done()
}
install_requirements_from_cran(packages)

warnings()

library("devtools")
install_github("datashield/dsBaseClient")
install_github("datashield/dsMediationClient", ref = "0.0.3")
install_github("https://github.com/transbioZI/dsMTLClient", ref = "0.9.9")
install_github("neelsoumya/dsSurvivalClient") # There is no version for this package
install_github("isglobal-brge/dsExposomeClient", ref = "2.0.8")

#check if all packages are installed
cli_alert_success("All packages are installed")
