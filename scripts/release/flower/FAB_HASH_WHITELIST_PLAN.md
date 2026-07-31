# FAB-hash whitelist — design plan (not yet implemented)

Status: agreed design, no code written yet. Companion repo: `molgenis-flwr-armadillo`.

## Root cause

Flower Hub's `fetch-fab` API never returns a `verifications` field to unlicensed callers:

```bash
curl -sS -X POST https://api.flower.ai/v1/hub/fetch-fab \
  -H "Content-Type: application/json" \
  -d '{"app_id":"@timmyjc/quickstart-pytorch-armadillo","app_version":"1.0.3","flwr_version":"1.32.1"}'
# -> 200, {"fab_url": "...", "app_version": "1.0.3", "resolved_by_compatibility": false, ...}
# no "verifications" field
```

Without it, the SuperLink sets `valid_license: ""` on the Fab record, and the stock
SuperNode's `--trusted-entities` check hard-rejects before ever reaching the real (fully
open-source) Ed25519 signature check. Confirmed against the actual `flwr/supernode:1.32.1`
Docker image (not just the pip package):

- `start_client_internal.py` (~line 380): `if trusted_entities: if not fab.verifications.get("valid_license", ""): reject`
- `control_servicer.py` `_get_remote_fab` (~line 1283): `valid_license` is only set when Hub's
  response includes `verifications`

So stock signature-based FAB trust is unusable for Hub-sourced apps without a paid licence —
no client-side patch can fix this, because Hub simply never sends the signature data.

`molgenis-flwr-armadillo` already tried and abandoned a related fix once: `supernode_verify.py`
+ a home-grown "FAB signing CLI" (removed in `e46bcb36`/`43a8b6e3` when migrating to
stock-images-only). That patch only closed the `valid_license` *gate*; it never solved "how do
you get a real signature onto a Hub app" — it worked by never using Hub's own review/signing at
all (a fully custom submission path, bypassing `@account/app` Hub resolution). Not reusable as
is, since the goal now is to keep using the Hub app store for distribution.

## Agreed design

Enforce a **FAB content-hash whitelist** in a custom SuperExec image. The SuperNode stays
100% stock and untouched.

**Rejected alternatives, and why:**

- *App name/version whitelist alone* — `fab_id`/`fab_version` are read from the *submitted*
  FAB's own `pyproject.toml` (`get_fab_metadata`), self-declared and unverified. An attacker
  can simply name malicious code after a trusted app; the SuperLink records whatever the FAB
  itself claims to be.
- *Armadillo calling Hub's `fetch-fab` server-side* to resolve+hash apps on the DM's behalf —
  rejected for simplicity/security. It would add a new authenticated write endpoint, an
  outbound-HTTP/download-size attack surface, and a new exception hierarchy inside Armadillo's
  trusted backend, for no security benefit over the DM doing it locally.

**Where the hash comes from:** the DM downloads the FAB via the already-unlicensed
`fetch-fab` call, reviews the code themselves (the one genuinely irreducible manual step —
this replaces Hub's paywalled review, not just its signing), then runs a new thin CLI:

```
molgenis_approve_flwr_app <fab-file>
```

which:
1. computes SHA-256 of the file (the value actually used for enforcement), and
2. extracts `(fab_id, fab_version)` straight from the same file via Flower's own public
   `flwr.cli.config_utils.get_fab_metadata(fab_bytes)` — confirmed present with identical
   signature in both the plain pip package and the real `flwr/supernode:1.32.1` image —
   so hash and name/version can never drift apart, unlike asking the DM to retype them,
3. prints a ready-to-paste `{app_id, app_version, fab_hash}` YAML entry.

**Where it's checked:** inside the SuperExec container, via Flower's own plugin extension
point — a `VerifiedClientAppExecPlugin(ClientAppExecPlugin)` overriding `select_task` to
reject any candidate `Task` whose `.fab_hash` (already present on the protobuf, computed
server-side by the SuperLink from the actual submitted bytes — not attacker-influenceable)
isn't in the whitelist. No Flower code is patched — this is Flower's own published plugin
mechanism, invoked via a custom entrypoint (`run_superexec(plugin_class=...)`, the same
function stock `flower-superexec`'s CLI calls internally) instead of the stock CLI's
hardcoded plugin choice.

**Where it's stored:** a static YAML whitelist file mounted into the SuperExec container,
exactly like `trusted-entities.yaml` is mounted into the SuperNode today (same
`DockerService.addBindMount` validation helper — fails closed on missing/empty file).

## Concrete implementation plan

**`molgenis-flwr-armadillo` repo:**

1. `VerifiedClientAppExecPlugin(ClientAppExecPlugin)` — `select_task` checks `task.fab_hash`
   against a whitelist loaded at startup; logs rejections.
2. Thin entrypoint module (e.g. `molgenis_flwr_armadillo/verified_superexec.py`) — parses the
   same CLI args as stock `flower-superexec` plus a new `--fab-whitelist PATH`, calls
   `run_superexec(plugin_class=VerifiedClientAppExecPlugin, stub_class=ClientAppIoStub, ...)`.
3. Dockerfile `FROM flwr/superexec:1.32.1` (or extend the existing baked-deps
   `timmyjc/superexec-torch:1.32.1`), installs the package, `ENTRYPOINT` → the new script.
4. `molgenis_approve_flwr_app` CLI as described above.

**`molgenis-service-armadillo` repo (this repo)** — the Flower backend code currently only
exists on `feat/flower-containers`/`test/flower-release-test`:

1. Add `fabWhitelistPath` to `FlowerSuperexecContainerConfig` (mirrors `trustedEntitiesPath`
   on `FlowerSupernodeContainerConfig`), default e.g. `data/system/flower/fab-whitelist.yaml`.
2. Extend `DockerService.configureBindMounts` to handle `FlowerSuperexecContainerConfig` too,
   reusing `addBindMount` as-is.
3. Extend `DockerService.configureDockerCmd` to append `--fab-whitelist <container-path>` for
   superexec containers (mirrors supernode's auto-appended `--trusted-entities`).
4. Point the superexec container config at the new custom image instead of stock
   `flwr/superexec:1.32.1`.

**Deferred / phase 2:** `POST /flower/whitelisted-apps` Armadillo endpoint where the DM posts
the *already-computed* `{app_id, app_version, fab_hash}` triple (never raw app+version —
Armadillo must never be the one calling Hub). Just an upsert into the same whitelist file;
role-gated; validates `fab_hash` is 64 hex chars and `app_id` looks like `@publisher/name`;
follow the existing `ControllerExceptionHandler` pattern (see `24ef347e`,
`ConnectionCreationFailedException` → 503, for the house style: specific exception per failure
mode, narrow try/catch scope, plain-text body). No outbound network call from Armadillo itself
— that's the whole point of doing this as phase 2, only after the DM-side CLI exists.

## Known limitations, carried forward deliberately

- A rejected task just silently never launches — `select_task` returning `None` isn't relayed
  back over the Flower protocol to whoever submitted the run (`LaunchResult`/
  `_handle_launch_result`: `FAILED` only logs locally on the node). The submitter sees a hung
  run, not a clear error.
- Revoking an app+version doesn't stop already-launched tasks, only gates future ones.
- Whitelist is per-SuperExec-container (per node), not per-project. The existing per-project
  RBAC on `/flower/push-data` (`FlowerController`/`FlowerDataService`) still separately gates
  which project's data a whitelisted app can actually touch, so this is real defense in depth —
  but if one node ever serves multiple projects with different trust appetites, the whitelist
  itself is coarser than that.
- Only covers the ClientApp-side SuperExec (data-loading nodes). The ServerApp-side SuperExec
  (`ServerAppExecPlugin`, orchestration code) is a separate plugin type and would need the same
  treatment separately if wanted — not currently in scope.
- Hash-pinning proves content-identity, not safety — it replaces Hub's paywalled review with
  the DM's own manual review before running `molgenis_approve_flwr_app`. Nothing technical
  enforces that the review actually happens.
