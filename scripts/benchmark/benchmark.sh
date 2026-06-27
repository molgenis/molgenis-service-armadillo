#!/usr/bin/env bash
# ==============================================================================
# Opal-vs-Armadillo DataSHIELD benchmark entrypoint.
#
# Brings up Opal (Docker) + Armadillo (gradlew) on localhost, all carrying the
# SAME dsBase (the published, dsBase-pinned datashield/rock-base:<version> image),
# then runs the benchmark and writes results/ + plots.
#
#   ./benchmark.sh --opal-version <tag> --dsbase-version <X.Y.Z> [options]
#
# Required (or set in bench.env):
#   --opal-version <tag>      datashield/opal_citest:<tag>
#   --dsbase-version <X.Y.Z>  dsBase for Opal-rock, Armadillo profiles, client lib
# Options:
#   --arma-rock-image <img>   image the Armadillo profiles run (default from bench.env)
#   --reps <n> / --duration <s> / --speed-reps <n>
#   --probe | --survey | --speed | --all   (default: --all)
#   --skip-setup              skip data upload + workspace save
#   --down                    tear down Opal + Armadillo and exit
#   -h | --help
#
# Docker and ./gradlew must be runnable by you (a sandbox cannot). Default
# ARMA_AUTH=basic (admin/admin) on localhost; gradlew OIDC token auth fails
# outside a configured server.
# ==============================================================================
set -euo pipefail

BENCH_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB="$BENCH_DIR/lib"
RESULTS="$BENCH_DIR/results"
ARMADILLO_REPO="$(cd "$BENCH_DIR/../.." && pwd)"   # scripts/benchmark -> repo root
ARMA_PORT="${ARMA_PORT:-8081}"

usage() { awk 'NR>1 && /^#/ {sub(/^# ?/,""); print; next} NR>1 {exit}' "${BASH_SOURCE[0]}"; }

# --- defaults from bench.env (if present) -----------------------------------
if [ -f "$BENCH_DIR/bench.env" ]; then
  set -a; . "$BENCH_DIR/bench.env"; set +a
fi

# --- parse flags (override the env file) ------------------------------------
DOWN=0; SKIP_SETUP=0; MODE=all
while [ $# -gt 0 ]; do
  case "$1" in
    --opal-version)    OPAL_IMAGE_TAG="$2"; shift 2;;
    --dsbase-version)  DSBASE_VERSION="$2"; shift 2;;
    --arma-rock-image) ROCK_IMAGE="$2";     shift 2;;
    --reps)            REPS="$2";           shift 2;;
    --duration)        DURATION_SEC="$2";   shift 2;;
    --speed-reps)      SPEED_REPS="$2";     shift 2;;
    --probe)           MODE=probe;  shift;;
    --survey)          MODE=survey; shift;;
    --speed)           MODE=speed;  shift;;
    --all)             MODE=all;    shift;;
    --skip-setup)      SKIP_SETUP=1; shift;;
    --down)            DOWN=1; shift;;
    -h|--help)         usage; exit 0;;
    *) echo "unknown argument: $1" >&2; usage; exit 1;;
  esac
done

# --- derive + export config for compose and the R scripts -------------------
export OPAL_COMPOSE="$BENCH_DIR/opal/docker-compose.yml"
export BENCH_LIB="${BENCH_LIB:-$BENCH_DIR/.Rlib}"
export OUT_CSV="$RESULTS/rates.csv"
export ARMA_AUTH="${ARMA_AUTH:-basic}"

down() {
  echo "== Tearing down =="
  docker compose -f "$OPAL_COMPOSE" down || true
  if [ -f "$RESULTS/armadillo.pid" ]; then
    kill "$(cat "$RESULTS/armadillo.pid")" 2>/dev/null || true
    rm -f "$RESULTS/armadillo.pid"
  fi
}
if [ "$DOWN" = 1 ]; then down; exit 0; fi

: "${OPAL_IMAGE_TAG:?--opal-version <tag> (or OPAL_IMAGE_TAG in bench.env) is required}"
: "${DSBASE_VERSION:?--dsbase-version <X.Y.Z> (or DSBASE_VERSION in bench.env) is required}"
export OPAL_IMAGE_TAG DSBASE_VERSION
# Published, dsBase-version-pinned Rock image -- used by Opal's rock service AND
# the Armadillo profiles. Permissive disclosure is set per profile (profiles.R),
# so no custom image build is needed.
export ROCK_IMAGE="${ROCK_IMAGE:-datashield/rock-base:${DSBASE_VERSION}}"

# Vendored data fallback: if DSBASECLIENT_DATA isn't set but data/ holds fixtures.
if [ -z "${DSBASECLIENT_DATA:-}" ] && ls "$BENCH_DIR"/data/*/*.rda >/dev/null 2>&1; then
  export DSBASECLIENT_DATA="$BENCH_DIR/data"
fi

mkdir -p "$RESULTS"

wait_for() {  # name url
  local name="$1" url="$2" i
  printf 'Waiting for %s (%s) ' "$name" "$url"
  for i in $(seq 1 60); do
    if curl -fsS -o /dev/null "$url" 2>/dev/null; then echo " ready"; return 0; fi
    printf '.'; sleep 5
  done
  echo " TIMEOUT"; return 1
}

run_r() { ( cd "$LIB" && Rscript "$1" ); }   # run from lib/ so source("config.R") resolves

# --- Opal: up (pinned rock image, nothing to build) -------------------------
echo "== Opal (opal_citest:${OPAL_IMAGE_TAG}, rock ${ROCK_IMAGE}) =="
docker compose -f "$OPAL_COMPOSE" up -d
wait_for "Opal" "${OPAL_URL:-http://localhost:8080}"

# --- Armadillo: gradlew run on 8081 (avoids Opal's 8080) --------------------
echo "== Armadillo (port ${ARMA_PORT}) =="
( cd "$ARMADILLO_REPO" && SERVER_PORT="$ARMA_PORT" ./gradlew run > "$RESULTS/armadillo.log" 2>&1 & echo $! > "$RESULTS/armadillo.pid" )
echo "Armadillo PID $(cat "$RESULTS/armadillo.pid") (log: $RESULTS/armadillo.log)"
wait_for "Armadillo" "${ARMA_URL:-http://localhost:8081}/actuator/health"

# --- Ensure the default + rserve profiles exist on the matched Rock image ----
echo "== Profiles =="
run_r profiles.R

# --- Setup + benchmark ------------------------------------------------------
[ "$SKIP_SETUP" = 1 ] || { echo "== Setup =="; run_r setup.R; }
case "$MODE" in
  probe)  echo "== Probe ==";  run_r probe.R;;
  survey) echo "== Survey =="; run_r bench.R; run_r plot.R;;
  speed)  echo "== Speed ==";  run_r capture.R; run_r speed_true.R; run_r speed_client.R; run_r plot_compute.R;;
  all)
    echo "== Probe ==";  run_r probe.R
    echo "== Survey =="; run_r bench.R; run_r plot.R
    echo "== Speed ==";  run_r capture.R; run_r speed_true.R; run_r speed_client.R; run_r plot_compute.R;;
esac

echo "Done. Results + plots in $RESULTS"
echo "Tear down with: $0 --down"
