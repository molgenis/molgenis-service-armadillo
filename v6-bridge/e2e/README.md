# End-to-End Testing: Armadillo + Vantage6

This directory contains a docker-compose setup for testing the full
Armadillo-as-vantage6-node integration locally.

## Prerequisites

- Docker and Docker Compose installed
- Armadillo running locally with data in `../../data/`
- Python 3.10+ with `vantage6-client` installed (for submitting tasks)

## Quick Start

### 1. Start the vantage6 server

```bash
cd v6-bridge/e2e
docker compose up v6-server -d
```

Wait for it to be healthy:

```bash
docker compose ps  # should show v6-server as "healthy"
```

### 2. Set up the v6 server (first time only)

Use the vantage6 Python client to create an organization, collaboration,
and node API key:

```python
from vantage6.client import Client

# Connect as the root user (default password on fresh server)
client = Client("http://localhost:8000", "/api")
client.authenticate("root", "root")  # default credentials

# Create an organization
org = client.organization.create(name="Armadillo Test Org")
org_id = org["id"]

# Create a collaboration
collab = client.collaboration.create(
    name="Test Collaboration",
    organizations=[org_id],
    encrypted=False,
)
collab_id = collab["id"]

# Create a node for this organization
node = client.node.create(
    name="armadillo-node",
    organization=org_id,
    collaboration=collab_id,
)
api_key = node["api_key"]

print(f"Collaboration ID: {collab_id}")
print(f"Node API key: {api_key}")
```

### 3. Start the v6-bridge

```bash
V6_API_KEY="<paste-api-key>" \
V6_COLLABORATION_ID=1 \
V6_AUTHORIZED_PROJECTS="shared-test" \
docker compose up v6-bridge -d
```

### 4. Verify the bridge is connected

```bash
# Check health endpoint
curl http://localhost:8081/health

# Check readiness
curl http://localhost:8081/health/ready
```

### 5. Submit a test task

```python
from vantage6.client import Client

client = Client("http://localhost:8000", "/api")
client.authenticate("root", "root")

# Submit a simple algorithm task
task = client.task.create(
    collaboration=1,
    organizations=[1],
    name="test-summary",
    image="harbor2.vantage6.ai/demo/average",
    input_={"method": "average", "kwargs": {"column": "age"}},
)

# Wait for results
result = client.wait_for_results(task["id"])
print(result)
```

## Troubleshooting

- **Bridge can't connect:** Check that the v6 server is healthy and
  the API key is correct.
- **Algorithm containers fail:** Check Docker socket is mounted and
  the bridge container has access.
- **No data:** Make sure `../../data/` contains Parquet files in
  project directories matching `V6_AUTHORIZED_PROJECTS`.

## Cleanup

```bash
docker compose down -v
```
