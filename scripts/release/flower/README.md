# Flower Container Integration Test

Tests that Armadillo can manage Flower containers (supernode + superexec) via its Docker API.

## Prerequisites

- Docker running
- Java 17+
- `flwr` CLI: `pip install 'flwr==1.23.0'`

## Setup (one-time)

Build Armadillo and push the superexec image:

```bash
./gradlew bootJar
docker login
./scripts/release/flower/build-push-superexec.sh
```

## Run

```bash
./scripts/release/flower/test-flower-containers.sh
```

## What it does

1. Starts a Flower superlink (standalone Docker container)
2. Starts two Armadillo instances on ports 8080/8081 (simulating two data nodes)
3. Creates flower-supernode + flower-superexec configs via the Armadillo REST API
4. Starts all containers via the API — verifies they land on `flower-network`
5. Runs `flwr run --stream` to execute a federated PyTorch training job
6. Verifies all containers are healthy
7. Cleans up everything on exit

## Debugging

While the test is running, open a second terminal to tail all container logs:

```bash
./scripts/release/flower/logs.sh
```

This streams logs from all flower containers (superlink, supernodes, clientapps) into a single output, prefixed with the container name. Pass a number to control how many historical lines to show:

```bash
./scripts/release/flower/logs.sh 100    # last 100 lines per container
```

To check individual containers:

```bash
docker logs -f flower-test-superlink
docker logs -f flower-supernode-1
docker logs -f flower-supernode-2
docker logs -f flower-clientapp-1
docker logs -f flower-clientapp-2
```

Armadillo logs are in the test directory:

```bash
tail -f scripts/release/flower/armadillo1.log
tail -f scripts/release/flower/armadillo2.log
```
