# Demo Pre-Setup

Do these steps before the demo. They build artifacts, push images, and prepare the test data. The infrastructure itself is started live during the demo.

## Prerequisites

- Docker running
- Java 17+
- `flwr` CLI: `pip install 'flwr==1.23.0'`
- `molgenis-flwr-armadillo` installed: `pip install -e /path/to/molgenis-flwr-armadillo`
- `molgenis-flwr-armadillo` repo checked out alongside this repo
- Python with `torch` + `torchvision` (for test data generation)

## Step 1: Build Armadillo

```bash
./gradlew bootJar
```

## Step 2: Build and Push Docker Images

Armadillo pulls images on container start, so they must be in a registry.

```bash
docker login
./scripts/release/flower/build-push-superexec.sh
```

Then build and push the verified superlink and superexec images from the `molgenis-flwr-armadillo` repo:

```bash
cd /path/to/molgenis-flwr-armadillo
./docker/build-push-all.sh
```

This builds and pushes:
- `timmyjc/verified-superlink:test` — patched SuperLink that preserves FAB signature verifications
- `timmyjc/superexec-data-test:0.0.1` — patched SuperExec with baked-in dependencies and ServerApp log visibility

## Step 3: Prepare CIFAR10 Test Data

Downloads CIFAR10 and writes `cifar10_train.pt` / `cifar10_test.pt` next to the script. Slow and only needed once.

```bash
./scripts/release/flower/prepare-test-data.sh
```

The demo will upload these files to Armadillo at runtime.

## Viewing Logs

Tail all container logs at once:

```bash
./scripts/release/flower/logs.sh
```

## Cleanup

```bash
./scripts/release/flower/cleanup.sh
```

Or press Ctrl+C in the terminal running the setup script.