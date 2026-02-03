# Plan: Internal JWT Tokens for Resource Access

## Goal
Enable R `resourcer` package to fetch resources via internal short-lived JWT tokens with `ROLE_RESOURCE_VIEW` per-resource.

## Current State (spike branch)
- StorageController has `/projects/{project}/rawfiles/{object}` endpoint (incomplete)
- RExecutorServiceImpl has placeholder code for token generation
- JwtRolesExtractor has TODO comment about internal tokens
- Static `mySecretMap` workaround needs proper solution

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌──────────────────┐
│ R loadResource  │────▶│ RExecutorService│────▶│ResourceTokenSvc  │
│ (user JWT)      │     │ generates token │     │ creates JWT      │
└─────────────────┘     └─────────────────┘     └──────────────────┘
                                                        │
                               internal JWT token       │
                               (5 min, per-resource)    ▼
┌─────────────────┐     ┌─────────────────┐     ┌──────────────────┐
│ resourcer HTTP  │────▶│ StorageController│────▶│CompositeJwtDecode│
│ GET /resources  │     │ @PreAuthorize   │     │ internal+external│
└─────────────────┘     └─────────────────┘     └──────────────────┘
```

## Implementation Steps

### 1. ResourceTokenKeyPairConfig (new)
`armadillo/src/main/java/org/molgenis/armadillo/security/ResourceTokenKeyPairConfig.java`
- Generate RSA 2048 key pair at startup
- Bean `KeyPair resourceTokenKeyPair()`

### 2. ResourceTokenService (new)
`armadillo/src/main/java/org/molgenis/armadillo/security/ResourceTokenService.java`
- Inject `KeyPair`, create `NimbusJwtEncoder`
- `generateResourceToken(email, project, object)` → signed JWT
- `isInternalToken(jwt)` → check `iss == "armadillo-internal"`
- Claims: `iss`, `sub` (email), `resource_project`, `resource_object`, `exp` (5 min)

### 3. CompositeJwtDecoder
Modify `JwtDecoderConfig.java`:
- Inject `KeyPair`, create internal decoder with public key
- Return composite decoder: try internal first, fallback to external OIDC
- Internal validation: issuer = "armadillo-internal", signature, expiry

### 4. JwtRolesExtractor
Modify `JwtRolesExtractor.java`:
- Inject `ResourceTokenService`
- If internal token: return `ROLE_RESOURCE_VIEW_<PROJECT>_<OBJECT>` (normalized)
- Else: existing email-based role lookup

### 5. RExecutorService interface
Modify `r/src/main/java/org/molgenis/r/service/RExecutorService.java`:
- Add `project` and `objectName` params to `loadResource()`

### 6. RExecutorServiceImpl
Modify `r/src/main/java/org/molgenis/r/service/RExecutorServiceImpl.java`:
- Inject `ResourceTokenService`
- Generate internal token in `loadResource()`
- Pass token to resourcer, rewrite URL from `/objects/` to `/rawfiles/`
- Remove static `mySecretMap`
- Audit token generation via `Auditor` (inject, log GENERATE_RESOURCE_TOKEN event)

### 7. CommandsImpl
Modify `CommandsImpl.java`:
- Pass `project` and `objectName` to `rExecutorService.loadResource()`
- (Already extracts these at lines 177-179)

### 8. StorageController
Modify `StorageController.java`:
- Endpoint: `GET /projects/{project}/rawfiles/{object}`
- Auth: `@PreAuthorize("hasRole('ROLE_RESOURCE_VIEW_' + normalize(#project, #object))")`
- Add helper method `normalizeResourceName(project, object)`
- Remove `mySecretMap` usage

## Token Structure
```json
{
  "iss": "armadillo-internal",
  "sub": "user@example.com",
  "resource_project": "cohort1",
  "resource_object": "folder/dataset",
  "iat": 1706968800,
  "exp": 1706969100
}
```

## Role Naming
`ROLE_RESOURCE_VIEW_COHORT1_FOLDER_DATASET`
- Uppercase
- Non-alphanumeric → underscore

## Files to Modify
| File | Action |
|------|--------|
| `security/ResourceTokenKeyPairConfig.java` | Create |
| `security/ResourceTokenService.java` | Create |
| `security/JwtDecoderConfig.java` | Modify (composite) |
| `security/JwtRolesExtractor.java` | Modify (internal token) |
| `r/service/RExecutorService.java` | Modify (params) |
| `r/service/RExecutorServiceImpl.java` | Modify (token gen) |
| `command/impl/CommandsImpl.java` | Modify (pass params) |
| `controller/StorageController.java` | Modify (auth) |

## Verification
1. Unit tests for ResourceTokenService (encode/decode)
2. Unit tests for JwtRolesExtractor (internal vs external)
3. Integration test: load resource → check token in R env
4. Manual: resourcer fetches via internal token

## Design Decisions
- **Key pair**: Ephemeral (regenerated on restart) - simpler, tokens are short-lived anyway
- **Token validity**: Fixed 5 minutes (hardcoded)
- **Audit logging**: Yes - log token generation via existing Auditor service
