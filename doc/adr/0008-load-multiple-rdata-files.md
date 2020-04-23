# 6. As a researcher you are able to load multiple RData files

Date: 2020-04-23

## Status

Accepted

## Context



## Decision

We implement and endpoint to upload and load RData files in the MOLGENIS "Armadillo" service to manage data for the use in DataSHIELD.

## Consequences
- We need a client which is capable to deal with uploading RData files into the MOLGENIS "Armadillo" service
- MOLGENIS needs to have an RData download option to be able to communicate directly with the MOLGENIS "Armadillo" service
