# Load all required libraries for release testing
# Note: cli is loaded separately in release-test.R before this file is sourced

# for password prompt
library(getPass)
# for reading parquet files
library(arrow)
# for data manipulation
library(dplyr)
library(tibble)
library(purrr)
library(stringr)
# for doing api calls
library(httr)
# for downloads with progress
library(curl)
# for loading json to put to api
library(jsonlite)
# to post resource file async to server to be able to show spinner while loading
library(future)
# to test if url exists
library(RCurl)
# to generate random project names
library(stringi)
# armadillo/datashield libraries needed for testing
library(MolgenisArmadillo)
library(dsBaseClient)
library(DSI)
library(DSMolgenisArmadillo)
library(resourcer)
library(dsSurvivalClient)
library(dsMediationClient)
library(dsMTLClient)
library(dsTidyverseClient)
library(dsExposomeClient)
library(dsOmicsClient)
library(testthat)