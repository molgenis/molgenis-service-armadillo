# Flower Data Loading - Security Architecture

## Overview

This document describes how data loading in Flower federated learning mirrors the existing DataSHIELD `assign.table` pattern. The goal is to maintain the same security properties: authenticated requests, controlled data push, and immediate file deletion after loading into memory.

## Comparison Table

| Step | DataSHIELD (assign.table) | Flower (push-data) |
|------|---------------------------|----------------------|
| **1. Researcher authenticates** | Gets OIDC token | Gets OIDC token |
| **2. Researcher triggers** | `datashield.assign.table("D", "project/table")` | Submits job with `run_config={"token": "...", "project": "x", "resource": "y"}` |
| **3. Job arrives** | DSI sends HTTP to Armadillo | Flower delivers run_config to ClientApp |
| **4. HTTP request to Armadillo** | DSI calls `POST /load-table` | ClientApp calls `POST /flower/push-data` (custom endpoint) |
| **5. Spring Security validates** | Automatic | Automatic |
| **6. Authorization check** | `ROLE_<PROJECT>_RESEARCHER` | `ROLE_<PROJECT>_RESEARCHER` |
| **7. Read from storage** | `armadilloStorage.loadTable()` | `armadilloStorage.loadObject()` |
| **8. Data push** | Rserve `writeFile()` | `docker exec cat > /tmp/armadillo_data/<project>_<resource>` |
| **9. Read file as bytes** | *(handled internally by R)* | `load_data()` reads file into bytes (custom helper in `flwr_armadillo` package) |
| **10. File deleted** | `base::unlink()` | `filepath.unlink()` (inside `load_data()`) |
| **11. Parse bytes into memory** | `arrow::read_parquet()` → R dataframe | `pd.read_parquet(io.BytesIO(raw))` → pandas dataframe (or any format) |
| **12. Code execution** | DataSHIELD functions (file gone, data in memory) | `model.fit(df)` (file gone, data in memory) |
| **13. Process ends** | Session ends | ClientApp exits |
| **14. Container stops** | Armadillo stops R container | Container stops |

## File Lifetime - Identical Pattern

```
DataSHIELD:
──────────────────────────────────────────────────────────────────►
     │         │         │
   Write     Load    Delete                      Researcher code
     │         │         │                              │
     └────┬────┴────┬────┘                              │
      File exists   └──── Data in memory only ──────────┘


Flower:
──────────────────────────────────────────────────────────────────►
     │       │       │       │
   Write   Read   Delete   Parse                  Researcher code
           bytes          bytes                         │
     │       │       │       │                          │
     └───┬───┴───┬───┘       └── Data in memory only ───┘
     File exists
```

In both cases, the file is deleted immediately after loading into memory, before the researcher's code runs.

## Side-by-Side Flow

```
DataSHIELD:                              Flower:

Researcher                               Researcher
    │                                        │
    │ datashield.assign.table()              │ job.submit(run_config={...})
    ▼                                        ▼
DSI (R package)                          Flower protocol
    │                                        │
    │ POST /load-table                       │
    │ + OIDC token                           ▼
    ▼                                    ClientApp starts
Armadillo                                    │
    │                                        │ load_data(context)
    │ Spring Security ✓                      │
    │ Authorization ✓                        │ POST /flower/push-data
    │                                        │ + OIDC token
    │                                        ▼
    │                                    Armadillo
    │                                        │
    │                                        │ Spring Security ✓
    │                                        │ Authorization ✓
    ▼                                        ▼
Rserve writeFile()                       docker exec cat > /tmp/...
    │                                        │
    ▼                                        ▼
R session                                ClientApp
    │                                        │
    │ read_parquet()                         │ load_data() returns bytes
    │ unlink()                               │ (file already deleted)
    ▼                                        ▼
DataSHIELD functions                     model.fit(df)
```

## Security Properties

| Property | DataSHIELD | Flower |
|----------|------------|--------|
| **Authentication** | OIDC token validated by Spring Security | OIDC token validated by Spring Security |
| **Authorization** | `ROLE_<PROJECT>_RESEARCHER` | `ROLE_<PROJECT>_RESEARCHER` |
| **Data push mechanism** | Rserve protocol | Docker exec |
| **File lifetime** | Milliseconds | Milliseconds |
| **Credentials in container?** | No | No |
| **Who controls file deletion?** | Armadillo (via Rserve) | Our helper library |

## Custom Components to Build

### 1. Armadillo Endpoint (Java)

```java
@RestController
@RequestMapping("/flower")
public class FlowerController {

    @PostMapping("/push-data")
    public ResponseEntity<?> pushData(
            @RequestBody PushDataRequest request,
            Principal principal) {

        // Spring Security already validated OIDC token

        // Check authorization
        String project = request.getProject();
        // ... verify user has ROLE_<PROJECT>_RESEARCHER ...

        // Read from storage
        InputStream data = armadilloStorage.loadObject(project, request.getResource());

        // Push to container via docker exec
        String containerId = getClientAppContainerId(request);
        dockerClient.execCreateCmd(containerId)
            .withCmd("sh", "-c", "cat > /tmp/armadillo_data/" + project + "_" + request.getResource())
            .withAttachStdin(true)
            .exec();
        // Stream data to stdin...

        return ResponseEntity.ok().build();
    }
}
```

### 2. Python Helper Library (`flwr_armadillo`)

```python
# flwr_armadillo/data.py
import requests
import os
import time
from pathlib import Path

DATA_DIR = Path("/tmp/armadillo_data")
ARMADILLO_URL = "http://host.docker.internal:8080"

def load_data(context) -> bytes:
    """
    Request data from Armadillo, load into memory, delete file.
    Returns raw bytes that can be parsed by any library.
    """
    token = context.run_config["token"]
    project = context.run_config["project"]
    resource = context.run_config["resource"]

    # 1. Call Armadillo to push data
    requests.post(
        f"{ARMADILLO_URL}/flower/push-data",
        headers={"Authorization": f"Bearer {token}"},
        json={"project": project, "resource": resource}
    )

    # 2. Wait for file to arrive
    filepath = DATA_DIR / f"{project}_{resource}"
    while not filepath.exists():
        time.sleep(0.1)

    # 3. Read file into memory as bytes
    with open(filepath, "rb") as f:
        raw_bytes = f.read()

    # 4. Delete file immediately
    filepath.unlink()

    # 5. Return bytes (file is gone, data in memory)
    return raw_bytes
```

### 3. Researcher's ClientApp Code

```python
from flwr.client import ClientApp
from flwr_armadillo import load_data
import pandas as pd
import io

app = ClientApp()

@app.train()
def train(context):
    # Get raw bytes (file already deleted inside load_data)
    raw = load_data(context)

    # Parse in any format needed
    df = pd.read_parquet(io.BytesIO(raw))

    # Or for other formats:
    # arr = np.load(io.BytesIO(raw))
    # img = Image.open(io.BytesIO(raw))
    # data = torch.load(io.BytesIO(raw))

    # Train model
    model.fit(df)
    return model.get_weights()
```

## How Bytes Loading Works

Every file is a sequence of bytes. By reading the file into memory as bytes before parsing, we can delete the file immediately:

```
┌─────────────────┐      f.read()      ┌─────────────────┐      pd.read_parquet()      ┌─────────────────┐
│ data.parquet    │ ─────────────────► │ bytes           │ ──────────────────────────► │ DataFrame       │
│ (file on disk)  │                    │ (in memory)     │                             │ (in memory)     │
└─────────────────┘                    └─────────────────┘                             └─────────────────┘
                                              │
                                              │ File deleted here
                                              │ Data safe in memory
                                              ▼
                                       filepath.unlink()
```

Most Python libraries (pandas, numpy, PIL, torch) can read from a `BytesIO` object, which wraps raw bytes as a file-like object.

## Manual Review Checklist

Since file deletion is handled by our helper library, the manual review process should verify:

- [ ] Uses `flwr_armadillo.load_data(context)` to get data
- [ ] Does not access `/tmp/armadillo_data/` directly
- [ ] No hardcoded file paths
- [ ] No exfiltration of raw bytes

## Estimated Effort

| Component | Complexity | Notes |
|-----------|------------|-------|
| `FlowerController` (Java) | Low | Single endpoint, reuses existing auth |
| `FlowerDataService` (Java) | Low | Docker exec, reuses storage service |
| `flwr_armadillo` (Python) | Low | ~50 lines of code |
| Docker image changes | Minimal | Ensure `/tmp/armadillo_data/` exists |

This is not a large amount of work as it reuses:
- Existing Spring Security infrastructure
- Existing storage service (`armadilloStorage`)
- Existing Docker client in Armadillo