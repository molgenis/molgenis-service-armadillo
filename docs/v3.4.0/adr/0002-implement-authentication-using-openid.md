# 2. Implement authentication using openid

Date: 2020-04-01

## Status

Accepted

## Context

We need a way to help users authentication in our system

## Decision

We implement openid in our application so we use ID-providers to authenticate in our services.
We intent to use JWTs which are machine-readable bearer tokens, to integrate external services.

## Consequences

- Use an ID-provider to authenticate in the DataSHIELD service.
- We intend to use the role mechanism in the ID-provider to authorise. The roles that we can think of at this moment are:
  - Local data manager
  - Researcher
  - Administrator
  This list can be updated and / or amended over time.
- By using an ID-provider we intend to be able to attach other systems as well.
- By using an ID-provider we intend to be able to federate to other ID-providers.