# Login backoff plan

## Goal
Lock the admin account after 5 failed login attempts, with exponential backoff. Reset on success. UI shows a countdown. State is in-memory (resets on restart).

## Backoff schedule
- Attempts 1–5: free
- 6th: 1 min
- 7th: 2 min
- 8th: 4 min
- 9th: 8 min
- 10th: 16 min
- 11th+: 30 min (cap)

Constants: `FREE_ATTEMPTS = 5`, `BASE_LOCKOUT = 1 min`, `MAX_LOCKOUT = 30 min`. Per-user.

---

## Backend

### 1. `LoginAttemptTracker` (new)
`armadillo/src/main/java/org/molgenis/armadillo/security/LoginAttemptTracker.java`

`@Component` holding `failedAttempts` (int) and `lockedUntil` (Instant). All methods `synchronized`:
- `recordFailure()` — increment; if over `FREE_ATTEMPTS`, set `lockedUntil = now + BASE_LOCKOUT * 2^(over-1)`, capped at `MAX_LOCKOUT`. Log a warning.
- `recordSuccess()` — reset both fields.
- `isLocked()` — `now < lockedUntil`.
- `getLockedUntil()` — exposed for the entry point.

### 2. `LoginAttemptListener` (new)
`armadillo/src/main/java/org/molgenis/armadillo/security/LoginAttemptListener.java`

`@Component` with two `@EventListener` methods:
- `AuthenticationFailureBadCredentialsEvent` → `tracker.recordFailure()`
- `AuthenticationSuccessEvent` → `tracker.recordSuccess()`

Do **not** listen for `AuthenticationFailureLockedEvent` — locked attempts must not extend the lockout.

### 3. Update `userDetailsService()` in `AuthConfig.java:170-180`
- Inject `LoginAttemptTracker` as a method parameter.
- Hash the password once into a local variable, outside the lambda.
- Replace `InMemoryUserDetailsManager` with a lambda that rebuilds the `User` on every call, with `.accountLocked(tracker.isLocked())`.

This makes Spring re-evaluate the lock state on every login, so the flag flips back to `false` automatically when the window expires.

### 4. Update `NoPopupBasicAuthenticationEntryPoint`
`armadillo/src/main/java/org/molgenis/armadillo/security/NoPopupBasicAuthenticationEntryPoint.java`

- Make it a `@Component` (or bean in `AuthConfig`) that holds a `LoginAttemptTracker`.
- In `commence()`, if `authException instanceof LockedException`:
  - Set `Content-Type: application/json`.
  - Set `Retry-After` header to seconds remaining.
  - Write JSON body: `{"locked": true, "lockedUntil": "<ISO instant>", "secondsRemaining": <int>}`.
- All other failures: bare 401 as today.
- Always status 401, never `WWW-Authenticate`.

### 5. Wire the entry point in `AuthConfig.java:104`
Replace `new NoPopupBasicAuthenticationEntryPoint()` with the injected bean.

---

## UI

### 6. Login page (`/basic-login`)
- Initial render: show form normally.
- On submit: POST credentials.
- On 401: parse body. If JSON with `locked: true`, switch to lockout view using `secondsRemaining`. Otherwise show "Wrong username or password".
- Lockout view: live countdown via `setInterval` showing `M:SS`. On reaching zero, re-enable the form locally.

---

## Tests

### 7. Release test in `scripts/release/testthat/tests/test-09-permissions.R`
Add a `test_that(...)` block that:
1. Sends 5 basic-auth requests with the wrong password — each returns plain 401.
2. Sends a 6th — asserts 401 **with** JSON body containing `locked: true` and `secondsRemaining > 0`, plus a `Retry-After` header.
3. (Optional) Sends a 7th with the **correct** password — still 401 with `locked: true`.

Notes:
- Place this test **last** in the file to avoid locking out subsequent tests.
- Use `httr::authenticate(user, pwd, type = "basic")` instead of the bearer helpers.
- Skip in `ADMIN_MODE` if that bypasses basic auth.

---

## Implementation order
1. `LoginAttemptTracker` + unit test for backoff math.
2. `LoginAttemptListener`.
3. Update `userDetailsService()` in `AuthConfig`.
4. Curl check: 5 wrong attempts, 6th locks even with right password, unlocks after 1 min.
5. Update `NoPopupBasicAuthenticationEntryPoint`.
6. Wire as bean in `AuthConfig`.
7. Curl check: confirm 6th 401 has JSON body + `Retry-After`; normal 401s do not.
8. UI lockout view + countdown.
9. Browser end-to-end check.
10. Add release test in `test-09-permissions.R`.

---

## Decisions
- Cap: 30 min. Base: 1 min. Free: 5.
- In-memory only, resets on restart.
- No "attempts remaining" hint.
- Per-user.
- No new endpoint — lockout info rides on the existing 401.
- Release test added to existing permissions file, runs last.

## Not doing
- Persistence.
- New endpoints or controllers.
- Custom failure handler / filter.
- Third-party rate-limit library.
- Per-IP tracking.
- Listener for `AuthenticationFailureLockedEvent`.