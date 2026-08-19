#!/usr/bin/env bash
# Run the DataSHIELD benchmark stages. ALL config lives in scripts/benchmark/.env
# (the single source of truth, read by lib/config.R); this launcher only points at
# it and runs one or more stages from lib/.
#
# Prereqs: .Rlib built (./install_benchmark_dependencies.R) and .env filled in.
# Local servers up (./start_servers.sh) if you include the *_local backends.
#
#   CHECK=1 bash run_benchmark.sh measure.R          # validate calls, no timing
#   bash run_benchmark.sh setup.R measure.R plots.R
#
# 'run_benchmark' is in the local hook's unsandboxed allowlist (the R client can't
# auth through the sandbox proxy).
set -euo pipefail

BENCH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export ENV_FILE="${ENV_FILE:-$BENCH/.env}"
export BENCH_LIB="${BENCH_LIB:-$BENCH/.Rlib}"
export RESULTS_DIR="${RESULTS_DIR:-$BENCH/results}"
export DATA_FILE="${DATA_FILE:-$BENCH/data/tables.rda}"

cd "$BENCH/lib"
for stage in "$@"; do
  echo "== $stage =="
  Rscript "$stage"
done
