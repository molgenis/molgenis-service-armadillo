# Flower End-to-End Test

This test simulates a real federated learning scenario: two Armadillo nodes with OIDC authentication, verified supernodes that check FAB signatures, and data loaded from Armadillo storage.

You manually generate keys, sign the FAB, start the infrastructure, then run each test scenario.

## Prerequisites

- Docker running
- Java 17+
- `flwr` CLI: `pip install 'flwr==1.23.0'`
- `molgenis-flwr-armadillo` installed: `pip install -e /path/to/molgenis-flwr-armadillo`
- `molgenis-flwr-armadillo` repo checked out alongside this repo
- Python with `torch` + `torchvision` (for test data generation)

## One-Time Setup

```bash
./gradlew bootJar
docker login
./scripts/release/flower/build-push-superexec.sh
```

## Step 1: Generate a Consortium Keypair

```bash
molgenis-flwr-keygen --name /tmp/consortium
```

Creates `/tmp/consortium.key` (private) and `/tmp/consortium.pub` (public), and prints the `key_id`.

## Step 2: Create `trusted-entities.yaml`

```bash
python3 -c "
from pathlib import Path; import yaml
from cryptography.hazmat.primitives.serialization import load_pem_public_key
from molgenis_flwr_armadillo.signing import derive_key_id
pub = Path('/tmp/consortium.pub').read_bytes()
kid = derive_key_id(load_pem_public_key(pub))
Path('/tmp/trusted-entities.yaml').write_text(yaml.dump({kid: pub.decode()}, default_flow_style=False))
print(f'Written /tmp/trusted-entities.yaml with key {kid}')
"
```

## Step 3: Build and Push the Verified Supernode Image

Armadillo pulls images on container start, so the image must be in a registry.

```bash
cd /Users/tcadman/github-repos/ds-molgenis/molgenis-flwr-armadillo
docker login
./docker/build-verified-images.sh
```

This builds and pushes `timmyjc/verified-supernode:test`. If the image already exists on Docker Hub, it skips.

## Step 4: Sign the Flower App

Run from the `molgenis-service-armadillo` repo root:

```bash
cd /Users/tcadman/github-repos/ds-molgenis/molgenis-service-armadillo
molgenis-flwr-sign \
  --app-dir scripts/release/flower/quickstart-pytorch-data \
  --private-key /tmp/consortium.key \
  --output /tmp/study.sfab
```

## Step 5: Start the Infrastructure

```bash
./scripts/release/flower/test-flower-containers.sh
```

This starts everything and waits:

1. Generates CIFAR10 test data
2. Starts a SuperLink (ports 9091-9093)
3. Starts two Armadillo instances with OIDC (ports 8080/8081)
4. Uploads test data to both Armadillo nodes
5. Authenticates via browser (`molgenis-flwr-authenticate` — opens a browser for each node)
6. Creates verified supernodes + clientapps via Armadillo API
7. Starts a serverapp superexec

Leave it running and open a new terminal for the test scenarios.

## Step 6: Run the Test Scenarios

Run each scenario from the repo root. Each script reads tokens from the temp file created during authentication.

### Scenario A: Signed FAB + correct tokens (should succeed)

Full end-to-end: signed FAB passes verification, valid tokens authenticate with Armadillo, data loads, training completes.

```bash
./scripts/release/flower/test-a-signed-fab-correct-tokens.sh
```

### Scenario B: Signed FAB + wrong token (should fail auth)

FAB passes signature check but Armadillo rejects the invalid token on data load.

```bash
./scripts/release/flower/test-b-signed-fab-wrong-token.sh
```

### Scenario C: Signed FAB + wrong project (should fail auth)

Valid tokens but requesting data from a project the user doesn't have access to.

```bash
./scripts/release/flower/test-c-signed-fab-wrong-project.sh
```

### Scenario D: Unsigned FAB (should be rejected by supernodes)

The verified supernodes reject the FAB because it has no valid signature. Check the supernode logs to confirm.

```bash
./scripts/release/flower/test-d-unsigned-fab.sh
```

### Scenario E: Signed FAB + no tokens (should fail)

Signed FAB passes verification but no tokens are provided, so data loading fails.

```bash
./scripts/release/flower/test-e-signed-fab-no-tokens.sh
```

## Viewing Logs

Tail all container logs at once:

```bash
./scripts/release/flower/logs.sh
./scripts/release/flower/logs.sh 100    # last 100 lines per container
```

Individual containers:

```bash
docker logs -f flower-test-superlink
docker logs -f flower-supernode-1
docker logs -f flower-supernode-2
docker logs -f flower-clientapp-1
docker logs -f flower-clientapp-2
docker logs -f flower-test-serverapp
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
