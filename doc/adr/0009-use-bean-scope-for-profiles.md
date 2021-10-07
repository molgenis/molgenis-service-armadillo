# 9. Use Spring bean scope to distinguish between profiles

Date: 2020-10-05

## Status

Proposed

## Context
We need to be able to switch between profiles.
But many singleton beans have profile information in them.

## Decision

Create a custom `@ProfileScope` to store the different versions of the profiles and inject proxies
to look up the correct instance.

We already did this for the ArmadilloSession, a `@SessionScope` bean, to store the connection to
the R server which is different for each user session.

## Consequences
- Profiles remain an aspect, are not interwoven with the logic
- Easier to test
- Once it works, it works
- The mechanism is harder to understand and debug