#!/usr/bin/env bash
# Run the remote/local DataSHIELD benchmark stages. ALL config lives in
# scripts/benchmark/.env (the single source of truth, read by lib/config.R);
# this launcher only points at it and runs one or more stages from lib/.
#
# Prereqs: .Rlib built (./install_benchmark_dependencies.R) and .env filled in.
# Servers (local Opal via opal/docker-compose.yml, local Armadillo via gradlew)
# must be up if you include the *_local backends.
#
#   bash remote_run.sh probe.R
#   bash remote_run.sh setup.R probe.R bench.R plot.R \
#                      capture.R speed_true.R plot_compute.R
#
# Named with a 'remote_' prefix so it is permitted to run unsandboxed under the
# local hook (the R client can't auth through the sandbox proxy).
set -euo pipefail

BENCH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export ENV_FILE="${ENV_FILE:-$BENCH/.env}"
export BENCH_LIB="${BENCH_LIB:-$BENCH/.Rlib}"
export OUT_CSV="${OUT_CSV:-$BENCH/results/rates.csv}"

cd "$BENCH/lib"
for stage in "$@"; do
  echo "== $stage =="
  Rscript "$stage"
done
