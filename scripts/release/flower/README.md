# Flower Data Loading Integration Test

Tests that Armadillo can push data into Flower containers via `POST /flower/push-data`, with full OIDC authentication.

## Prerequisites

- Docker running
- Java 17+
- `flwr` CLI: `pip install 'flwr==1.23.0'`
- `molgenis-flwr-armadillo` installed: `pip install -e ../../molgenis-flwr-armadillo`
- Python with `torch` and `torchvision` (for generating test data)
- `molgenis-flwr-armadillo` repo checked out alongside this repo

## Setup (one-time)

Build Armadillo and push the superexec image:

```bash
./gradlew bootJar
docker login
./scripts/release/flower/build-push-superexec.sh
```

## Run

```bash
./scripts/release/flower/test-flower-data.sh
```

The test is **interactive** — it will open a browser window for OIDC authentication to each Armadillo node (twice: once per node).

## What it does

1. Generates CIFAR10 test data subsets (`.pt` files)
2. Starts a Flower SuperLink
3. Starts two Armadillo instances (ports 8080/8081) with OIDC enabled
4. **Authenticates** via `molgenis-flwr-authenticate` (device flow, opens browser)
5. Uploads test data to both Armadillo instances' storage
6. Creates flower-supernode + flower-superexec (clientapp) containers via API
7. Starts all containers
8. Runs `flwr run --stream` with OIDC tokens and URLs passed via `--run-config`
9. ClientApps call `load_data()` which POSTs to `/flower/push-data` using the OIDC token — Armadillo validates the JWT, reads from storage, Docker-copies into the container
10. Verifies training completes and all containers are healthy
11. Cleans up everything on exit

## Data flow

```
ClientApp container           Armadillo (host)
    |                              |
    | POST /flower/push-data       |
    | {project, resource,          |
    |  containerName}              |
    | + Bearer OIDC token          |
    |----------------------------->|
    |                              | 1. Validates JWT (auth.molgenis.org)
    |                              | 2. Checks researcher role
    |                              | 3. Reads from storage
    |                              | 4. Docker copy into container
    |  204 No Content              |    at /tmp/armadillo_data/
    |<-----------------------------|
    |                              |
    | Python load_data():          |
    |   read file -> bytes         |
    |   delete file                |
```

## Authentication flow

```
1. Test script creates flower-nodes.yaml pointing to localhost:8080 and :8081
2. molgenis-flwr-authenticate reads the yaml
3. For each node, it calls GET /actuator/info to discover the OIDC issuer
4. Device flow auth opens browser -> user authenticates at auth.molgenis.org
5. Tokens saved to /tmp/flwr_tokens.json
6. Test script reads tokens and passes them via flwr run --run-config
7. ServerApp distributes tokens+URLs to ClientApps via extract_tokens()
8. ClientApps use get_node_token() and get_node_url() to get their credentials
```

## Debugging

Tail all container logs:

```bash
./scripts/release/flower/logs.sh
```

Individual containers:

```bash
docker logs -f flower-clientapp-1
docker logs -f flower-clientapp-2
```

Armadillo logs:

```bash
tail -f scripts/release/flower/armadillo1.log
tail -f scripts/release/flower/armadillo2.log
```
