# Flower End-to-End Test

This test simulates a real federated learning scenario using only stock
Flower components: two Armadillo nodes with OIDC authentication, a TLS
SuperLink, and supernodes that only run apps pulled from Flower Hub and
signed by a trusted reviewer.

Trust model: the app is published on Flower Hub (`flwr app publish`) and
reviewed/signed there (`flwr app review`). Armadillo starts each supernode
with `--trusted-entities`, listing the reviewer public keys the node
operator trusts. Supernodes reject any run whose FAB does not carry a
trusted signature — including FABs pushed directly to the SuperLink, which
never carry verifications.

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
username.

### 3. Review and sign the app

Register an Ed25519 public key on your Flower profile
(`https://flower.ai/profile/<username>/`), then:

```bash
flwr app review @timmyjc/quickstart-pytorch-armadillo==1.0.0
```

### 4. Configure the reviewer trust in `.env`

Copy `.env.dist` to `.env` and set:

- `REVIEWER_KEY_ID` — the key id shown in the app page's Verifications
  section on Flower Hub
- `REVIEWER_PUBLIC_KEY_FILE` — path to the matching Ed25519 OpenSSH public
  key file

## Step 1: Start the Infrastructure

```bash
./scripts/release/flower/test-flower-containers.sh
```

This starts everything and waits:

1. Generates TLS certs, supernode auth keys and trusted-entities.yaml
2. Generates CIFAR10 test data
3. Starts a TLS SuperLink (ports 9091-9093)
4. Starts two Armadillo instances with OIDC (ports 8080/8081), each
   configured with `flower.armadillo-url` so superexec containers know
   their node's URL
5. Uploads test data to both Armadillo nodes
6. Registers and starts supernodes + clientapps via the Armadillo API

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

### Scenario D: app not from the Hub (should be rejected by supernodes)

A locally-built FAB is pushed directly to the SuperLink; the supernodes
reject it with FAB_VERIFICATION_ERROR because directly-pushed FABs carry no
verifications. Check the supernode logs to confirm.

```bash
./scripts/release/flower/test-d-unverified-app.sh
```

### Scenario E: Hub app + no tokens (should fail)

```bash
./scripts/release/flower/test-e-hub-app-no-tokens.sh
```

### Scenario F: Hub app signed by an untrusted reviewer (should be rejected)

The app carries a valid Hub reviewer signature, but not from a key listed in
the nodes' trusted-entities.yaml. The supernodes reject the run with
FAB_VERIFICATION_ERROR. The script temporarily swaps trusted-entities.yaml
for a throwaway key, restarts the supernodes, and restores it afterwards.

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
