# Benchmark

A self-contained Opal-vs-Armadillo DataSHIELD benchmark. It brings up Opal
(Docker) and Armadillo (gradlew) on localhost — all carrying the **same dsBase**,
pinned at image build time — then measures `ds.*` / `datashield.*` performance and
writes CSVs + plots to `results/`.

Sibling of `release/`: vendored R under `lib/`, an executable entrypoint, an
`install_*` script, and a `bench.env.dist`. Vendored (not a submodule) so it runs
from a fresh checkout. See `docs/armadillo-integration-plan.md` in the
armadillo-opal-comparison repo for the design rationale.

## What it measures

Two scenarios over a shared op registry (`lib/ops.R`, 43 ops over 2 servers /
3 profiles — Opal, Armadillo `default`, Armadillo `rserve`):

1. **Broad throughput survey** (`lib/bench.R`) → `results/rates.csv`, plotted by
   `lib/plot.R` → `results/comparison.png`.
2. **Primitive true-vs-client speed** (`lib/capture.R` → `lib/speed_true.R` /
   `lib/speed_client.R`) → `results/speed_*.csv`, plotted by `lib/plot_compute.R`
   → `results/compute.png`. The gap is the DSI poll-sleep penalty.

`armadillo` and `armadillo_rserve` are **two compute profiles on one Armadillo**,
benchmarked one at a time (sequential, so the profiles don't contend).

## Version honesty (the point of this harness)

All backends carry the **same dsBase**, *selected* (not built) from DataSHIELD's
**published, dsBase-version-pinned** Rock images: `datashield/rock-base:${DSBASE_VERSION}`
(a tag exists per dsBase release, e.g. `6.3.5`). Opal's `rock` service and the
Armadillo `default`/`rserve` profiles all use it (`ROCK_IMAGE` / `--arma-rock-image`),
and the client `dsBaseClient` is installed at the same version. No runtime
`dsadmin.install_*`, and no custom image build — selecting the tag is enough.

**Permissive disclosure** (needed for `ds.recodeValues`, `ds.Boole`, …) is set per
profile via the `datashield.privacyControlLevel` option in `lib/profiles.R`, so the
stock pinned image works without a permissive build.

## Memory parity with Opal

Opal's Java server runs with `-Xms1G -Xmx2G -XX:+UseG1GC` (a 2 GB heap), bounded
inside the Docker VM. The local Armadillo, launched natively via `gradlew`, is
otherwise unbounded and can grow into host RAM — risking a macOS memory-pressure
(jetsam) kill of the JVM mid-run. To match Opal and stay safe, launch the Armadillo
with the same heap via `ARMA_JAVA_OPTS` (default `-Xms1G -Xmx2G -XX:+UseG1GC`):

- `benchmark.sh` applies it automatically to its `gradlew` launch.
- For the `remote_run.sh` flow (you start the servers yourself), start the local
  Armadillo with:

  ```bash
  JAVA_TOOL_OPTIONS="-Xms1G -Xmx2G -XX:+UseG1GC" SERVER_PORT=8081 ./gradlew run
  ```

The DataSHIELD compute for both backends runs in Docker Rock containers (equally
VM-bounded), so this heap cap is the one meaningful memory-parity knob. CPU needs no
pinning: the survey runs cells sequentially (one backend at a time), so backends
never contend during measurement.

## Once

```bash
DSBASE_VERSION=6.3.5 ./install_benchmark_dependencies.R   # -> .Rlib
cp bench.env.dist bench.env                                # then edit
```

`install_*` and `benchmark.sh` share `BENCH_LIB` (default `.Rlib`).

## Run

```bash
./benchmark.sh --opal-version latest --dsbase-version 6.3.5
```

Both flags are required (or set `OPAL_IMAGE_TAG` / `DSBASE_VERSION` in `bench.env`).
`benchmark.sh` resolves `ROCK_IMAGE=datashield/rock-base:${DSBASE_VERSION}`, starts
Opal, starts Armadillo on 8081, ensures the profiles permissive (`lib/profiles.R`),
then runs setup + the benchmark.

Useful flags: `--probe` | `--survey` | `--speed` | `--all` (default),
`--reps N`, `--duration S`, `--speed-reps N`, `--skip-setup`, `--down` (teardown).

Quick smoke:

```bash
./benchmark.sh --opal-version latest --dsbase-version 6.3.5 --probe
./benchmark.sh --opal-version latest --dsbase-version 6.3.5 --survey --duration 2 --reps 1
```

## Remote / multi-server runs (`.env`-driven, resumable)

To compare **local *and* remote** servers, skip `benchmark.sh` (it only starts
localhost Opal+Armadillo) and drive the R stages directly with `remote_run.sh`.
All config comes from a single git-ignored `.env` (read by `lib/config.R`) — no
URLs/credentials are hardcoded. Backends are named `<kind>_<location>[_rserve]`:

```
opal_local, opal_remote,
armadillo_local, armadillo_local_rserve,
armadillo_remote, armadillo_remote_rserve
```

`.env` keys: `OPAL_{LOCAL,REMOTE}_{URL,USER,PASS}`,
`ARMA_{LOCAL,REMOTE}_{URL,USER,PASS,AUTH}`, `ARMA_PROFILE`, `ARMA_RSERVE_PROFILE`,
`BACKENDS`, plus run params (`N_ROWS`, `N_VARS`, `DURATION_SEC`, `REPS`,
`SPEED_REPS`, `POLL_SLEEP0`). Missing connection keys fail loudly. All datasets are
always uploaded/assigned (there is no dataset-subsetting knob).

Run one or more stages (config auto-loaded from `.env`):

```bash
bash remote_run.sh setup.R probe.R
bash remote_run.sh bench.R plot.R capture.R speed_true.R speed_client.R plot_compute.R
```

### Resume an interrupted run

Results are written **incrementally** — `bench.R` appends each `(backend, op, rep)`
cell to `results/rates.csv` the moment it completes, so a crash never loses
finished cells. To continue, relaunch with `RESUME=1`:

```bash
RESUME=1 bash remote_run.sh bench.R plot.R capture.R speed_true.R speed_client.R plot_compute.R
```

`RESUME=1` reads the existing `rates.csv`, **skips every cell already recorded**, and
appends only the rest. It also backfills a backend that was down: once it recovers,
add it back to `BACKENDS` and rerun with `RESUME=1` — only that backend's cells run.
Without `RESUME` a run starts fresh and **overwrites** `rates.csv`. (The speed suite
is short — just re-run it; only the survey is resumable.)

### Orphaned sessions (run after a kill)

A hard-killed run never reaches `logout_all()`, so its server-side DataSHIELD R
sessions linger — each holds a profile R-worker slot, so a server (especially a
shared Opal) can begin **hanging** new commands. Clear them before relaunching:

```bash
bash remote_run.sh cleanup_sessions.R
```

### Keep the machine awake

Everything runs locally, so if the laptop sleeps the run and the local servers die
and connections drop. Prevent sleep from **your own terminal** with
`caffeinate -dimsu` (a *sandboxed* caffeinate is a no-op — it can't hold the power
assertion), lid open and on AC power. Verify with
`pmset -g assertions | grep caffeinate`.

## Data

The benchmark inflates real dsBaseClient test data (`tests/testthat/data_files`)
to `N_ROWS`×`N_VARS`. Set `DSBASECLIENT_DATA` to that directory, or vendor the
`.rda` fixtures under `data/` (license permitting) — `benchmark.sh` falls back to
`data/` automatically.

## Caveats

- **Docker + gradlew are user-run** (a sandbox can't); on localhost use
  `ARMA_AUTH=basic`.
- **Opal OOM** — `datashield/opal_citest` can OOM (exit 137) under load; the survey
  self-heals via `OPAL_COMPOSE` restarts on long runs.
- **`:::` private-API readers** in the true-compute path can break across driver
  versions — they're `tryCatch`→`NA` and version-pinned.
- This is **measurement, not a pass/fail suite** — a dropped backend is healed and
  re-queued, not a test failure (see the integration plan's testthat-fit section).
