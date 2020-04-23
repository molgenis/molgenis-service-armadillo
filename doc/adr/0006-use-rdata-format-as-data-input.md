# 6. Use RData file format as data input for the service

Date: 2020-04-23

## Status

Accepted

## Context

We want to make the MOLGENIS "Armadillo" service data provider agnostic. There are a couple of reasons why we are doing this
- the service is usable for other parties as well
- the service can still integrate with MOLGENIS. 
- the release cycle of the service is data provider independent
- the service can be developed by other parties as well

## Decision

We implement and endpoint to upload and load RData files in the MOLGENIS "Armadillo" service to manage data for the use in DataSHIELD.

## Consequences
- We need a client which is capable to deal with uploading RData files into the MOLGENIS "Armadillo" service
- MOLGENIS needs to have an RData download option to be able to communicate directly with the MOLGENIS "Armadillo" service
