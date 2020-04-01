# 2. Implement authentication using openid

Date: 2020-04-01

## Status

Accepted

## Context

We need a way to help users authentication in our system

## Decision

We implement openid in our application so we use ID-providers to authenticate in our services.
Use JWTs which are machine-readable bearer tokens, to integrate external services.

## Consequences

- Use an ID-provider to authenticate in the DataSHIELD service
- We use to some extend the roles in the ID-provider to authorise
  - Local data manager
  - Researcher
  - Administrator
- Being able to attach other systems as well
- Being able to federate to other ID-providers