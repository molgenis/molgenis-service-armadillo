# 4. Implement asynchronicity in the requestflow

Date: 2020-04-01

## Status

Accepted

## Context

Clients can have operations running on multiple DataSHIELD servers concurrently.

## Decision

We need to support asynchronous requests in the R client. We implemented it using completable futures.
We must keep the last execution result for each R session until it gets retrieved or until a new execution is started.

## Consequences

- Users can have more than one session on the same server.
