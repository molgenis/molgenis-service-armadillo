# Spring Security 7 — Security Context Bleed

## Problem

After migrating to Spring Boot 4 (which includes Spring Security 7), researchers are getting admin rights they shouldn't have.

## Root cause

Spring Security 7 introduced Multi-Factor Authentication support with a new `FactorGrantedAuthority` system. This uses an **additive model** — when multiple authentication methods are used on the same session, authorities **accumulate** rather than replace each other.

In Armadillo, basic auth (admin) and JWT bearer tokens (researchers) share a single `SecurityFilterChain` with session storage enabled. When an admin logs in via basic auth, `ROLE_SU` is saved to the session. When a researcher subsequently authenticates via JWT bearer token, SS7 **adds** their researcher role to the session instead of replacing it. The researcher ends up with both `ROLE_SU` and their own `ROLE_*_RESEARCHER`.

In Spring Security 6, bearer token auth was stateless by default and ignored the session. SS7's factor accumulation changed this behavior.

## Proposed fix

Split the single `SecurityFilterChain` into two:

1. **Stateless chain** (for JWT bearer token requests): `SessionCreationPolicy.STATELESS`, no session interaction. Handles API calls from R/Python clients.
2. **Stateful chain** (for browser requests): Sessions enabled for basic auth and OIDC login. Handles the Armadillo UI.

This ensures bearer token requests never read from or write to the session, eliminating the authority bleed. This is the standard Spring Security pattern for apps that serve both API clients and browser users.

## Evidence

Audit logs show the problem clearly — within a single request, the JWT authentication succeeds with correct (limited) authorities, but by the time the controller executes, the session's basic auth authorities have been merged in:

```
AUTHENTICATION_SUCCESS authorities=[ROLE_76DCL55YUQ_RESEARCHER, FACTOR_BEARER]
LIST_ACCESS_DATA       roles=[ROLE_76DCL55YUQ_RESEARCHER, FACTOR_BEARER, ROLE_SU, FACTOR_PASSWORD]
```

## Files involved

- `armadillo/src/main/java/org/molgenis/armadillo/security/AuthConfig.java` — single SecurityFilterChain to be split
- `armadillo/src/main/java/org/molgenis/armadillo/security/RunAs.java` — uses SecurityContextHolder directly (review for SS7 compatibility)
