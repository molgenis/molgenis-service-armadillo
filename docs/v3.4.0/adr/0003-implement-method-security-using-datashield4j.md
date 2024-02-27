# 3. Implement method security using datashield4j

Date: 2020-04-01

## Status

Accepted

## Context

We need some way of being sure that there is no malicious script execution on the DataSHIELD environment.

## Decision

We implement datashield4j to prevent users to execute method and parameters other than the DataSHIELD provided methods.
We looked at OCAP ([R native object capabilities](https://docs.google.com/document/d/1Yx10Xw8Uige3hK-6YwzM8RhrtqSogDTGAZlcF218U2U/edit)) as well but came to the conclusion 
that the client that OCAP is using needs to be developed in JAVA. The datashield4j library is already available and tested which makes it easier to implement.

## Consequences

- Users can not execute malicious scripts on the R environment anymore.
