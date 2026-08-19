# Flower End-to-End Test

This test simulates a real federated learning scenario using only stock
Flower components: two Armadillo nodes with OIDC authentication, a TLS
SuperLink, and clientapp SuperExec containers that only run apps whose FAB
content hash has been explicitly approved.

Trust model: the app is distributed via Flower Hub (`flwr app publish` /
`flwr run @account/app`), but Hub review/signing is **not** the trust
mechanism — Hub's `fetch-fab` API doesn't return signature data to
unlicensed callers, so stock signature verification can't work for Hub apps
(see `FAB_HASH_WHITELIST_PLAN.md`). Instead, each clientapp container runs a
custom SuperExec plugin (`armadillo-flwr-superexec`, from
`molgenis-flwr-armadillo`) that checks the task's FAB hash against a
whitelist. A data manager approves an app by downloading its FAB, reviewing
the code, and running `armadillo-flwr-approve-app` to compute the hash; this
test automates that step (`fetch_fab` + `approve_fab_entry_json` in
`config.sh`) and includes the approved entry directly in each clientapp's
`PUT /containers` body. Supernodes are stock, unmodified — they no longer do
any FAB verification at all; the check moved to the SuperExec.

## Prerequisites

- Docker running
- Java 17+
- `flwr` CLI matching the pinned version: `pip install 'flwr==1.32.1'`
- `molgenis-flwr-armadillo` installed (for `armadillo-flwr-authenticate`):
  `pip install -e /path/to/molgenis-flwr-armadillo`
- `molgenis-flwr-armadillo` repo checked out alongside this repo (the test
  app lives at `examples/pytorch-armadillo`)
- Python with `torch` + `torchvision` (for test data generation)
- `127.0.0.1 host.docker.internal` in `/etc/hosts` (containers and host must
  agree on each Armadillo's URL for token routing)

## One-Time Setup

### 1. Build Armadillo

```bash
./gradlew bootJar
```

### 2. Publish the app on Flower Hub

```bash
flwr login supergrid
flwr app publish ../molgenis-flwr-armadillo/examples/pytorch-armadillo
```

The publisher in the app's `pyproject.toml` must match your Flower account
username. Note the published version — `HUB_APP_VERSION` in `.env` must be
pinned to it exactly (see below); the whitelist is keyed by content hash, so
an unpinned "latest" resolution could approve a different build than the one
`flwr run` actually pulls.

Review/signing is not required — trust is the FAB-hash whitelist, computed
and approved automatically by `register-flower-containers.sh` (via
`fetch_fab` + `approve_fab_entry_json` in `config.sh`), not a Hub reviewer
signature. In production a data manager runs `armadillo-flwr-approve-app`
themselves after manually reviewing the downloaded FAB; this test automates
that same computation, skipping only the manual code review a real approval
requires.

### 3. Configure `.env`

Copy `.env.dist` to `.env` and set `HUB_APP`/`HUB_APP_VERSION` to match what
you published in step 2.

## Step 1: Start the Infrastructure

```bash
./scripts/release/flower/test-flower-containers.sh
```

This starts everything and waits:

1. Generates TLS certs and supernode auth keys
2. Generates CIFAR10 test data
3. Starts a TLS SuperLink (ports 9091-9093)
4. Starts two Armadillo instances with OIDC (ports 8080/8081), each
   configured with `flower.armadillo-url` so superexec containers know
   their node's URL
5. Uploads test data to both Armadillo nodes
6. Fetches the Hub app's FAB, hashes it, and registers + starts supernodes
   (stock) and clientapps (whitelist-enforcing image, pre-approved with that
   hash) via the Armadillo API

Leave it running and open a new terminal for authentication and the test
scenarios.

## Step 2: Authenticate

```bash
armadillo-flwr-authenticate --config scripts/release/flower/flower-nodes.yaml
```

Opens a browser for each node; tokens are stored keyed by sanitized URL.

## Step 3: Run the Test Scenarios

### Scenario A: Hub app + correct tokens (should succeed)

```bash
./scripts/release/flower/test-a-hub-app-correct-tokens.sh
```

### Scenario B: Hub app + wrong token (should fail auth)

```bash
./scripts/release/flower/test-b-hub-app-wrong-token.sh
```

### Scenario C: Hub app + wrong project (should fail auth)

```bash
./scripts/release/flower/test-c-hub-app-wrong-project.sh
```

### Scenario D: unwhitelisted FAB hash (should be rejected)

A freshly-built FAB of the same app is pushed directly to the SuperLink
instead of the Hub-fetched one that was approved. FAB builds aren't
byte-deterministic, so even identical source produces a different hash — the
SuperExec plugin rejects it via `PushTaskOutput` before any app code runs.
Check the clientapp container logs (not the supernode — the check now
happens at the SuperExec) to confirm.

```bash
./scripts/release/flower/test-d-unverified-app.sh
```

### Scenario E: Hub app + no tokens (should fail)

```bash
./scripts/release/flower/test-e-hub-app-no-tokens.sh
```

### Scenario F: previously-approved hash removed from the whitelist (should be rejected)

The app was approved in Step 1, but its whitelist entry is temporarily
swapped out for an unrelated one. The script replaces each clientapp's
`fab-whitelist.yaml`, restarts the clientapp containers so the plugin
re-reads it, runs the app (rejected — check clientapp logs), then restores
the original whitelist and restarts again.

```bash
./scripts/release/flower/test-f-hub-app-untrusted-reviewer.sh
```

## Viewing Logs

Tail all container logs at once:

```bash
./scripts/release/flower/logs.sh
./scripts/release/flower/logs.sh 100    # last 100 lines per container
```

Armadillo logs:

```bash
tail -f scripts/release/flower/armadillo1.log
tail -f scripts/release/flower/armadillo2.log
```

## Cleanup

```bash
./scripts/release/flower/cleanup.sh
```

Or press Ctrl+C in the terminal running the setup script.
