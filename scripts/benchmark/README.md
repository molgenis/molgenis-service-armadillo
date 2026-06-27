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
