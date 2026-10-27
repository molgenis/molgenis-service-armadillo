#!/usr/bin/env bash
# ==============================================================================
# Start the LOCAL benchmark servers, version-pinned from .env, and wait until both
# are healthy:
#   - Opal + Mongo + Rock via docker compose (detached)
#   - a released Armadillo jar on :8080 (background; logs to .armadillo/armadillo.log),
#     via run_local_armadillo.sh
#
#   bash start_servers.sh            # start both, wait for health
#   bash start_servers.sh --down     # stop both
#
# Versions + the Opal admin password come from .env (OPAL_VERSION, OPAL_ROCK_VERSION,
# ARMADILLO_VERSION, OPAL_LOCAL_PASS, ARMA_LOCAL_USER/PASS). Requires Docker + Java.
# Create the default + rserve Armadillo profiles once (see README). Remote servers
# are untouched.
# ==============================================================================
set -euo pipefail

BENCH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE="$BENCH/opal/docker-compose.yml"
ARMA_DIR="$BENCH/.armadillo"
ARMA_PID="$ARMA_DIR/armadillo.pid"
ARMA_LOG="$ARMA_DIR/armadillo.log"

# Read one KEY from .env (inline ' # comment' + whitespace stripped), like config.R.
read_env() {
  local k="$1" f="${ENV_FILE:-$BENCH/.env}"
  [ -f "$f" ] || return 0
  grep -E "^[[:space:]]*${k}=" "$f" | head -1 | sed -E "s/^[^=]*=//; s/[[:space:]]+#.*$//; s/[[:space:]]+$//"
}

wait_for() {  # name url
  local name="$1" url="$2" i
  printf 'Waiting for %s ' "$name"
  for i in $(seq 1 60); do
    if curl -fsS -o /dev/null "$url" 2>/dev/null; then echo " ready"; return 0; fi
    printf '.'; sleep 5
  done
  echo " TIMEOUT (check the logs)"; return 1
}

down() {
  docker compose -f "$COMPOSE" down || true
  # Stop Armadillo: by PID file if present, and (belt-and-braces) any host process
  # still listening on ARMA_LOCAL_PORT -- so a missing/stale PID file can't leave it up.
  local port pid
  port="$(read_env ARMA_LOCAL_PORT)"; port="${port:-8080}"
  if [ -f "$ARMA_PID" ]; then
    kill "$(cat "$ARMA_PID")" 2>/dev/null || true
    rm -f "$ARMA_PID"
  fi
  pid="$(lsof -nP -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | awk 'NR>1 {print $2; exit}')"
  if [ -n "${pid:-}" ]; then
    echo "stopping Armadillo (pid $pid on :$port)"
    kill "$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null || true
  fi
  echo "Local servers stopped."
}

if [ "${1:-}" = "--down" ]; then down; exit 0; fi

OPAL_LOCAL_PORT="$(read_env OPAL_LOCAL_PORT)"; OPAL_LOCAL_PORT="${OPAL_LOCAL_PORT:-8081}"
ARMA_LOCAL_PORT="$(read_env ARMA_LOCAL_PORT)"; ARMA_LOCAL_PORT="${ARMA_LOCAL_PORT:-8080}"
OPAL_URL="http://localhost:$OPAL_LOCAL_PORT"
ARMA_URL="http://localhost:$ARMA_LOCAL_PORT"

# --- Opal (detached) --------------------------------------------------------
export OPAL_LOCAL_PORT
export OPAL_VERSION="$(read_env OPAL_VERSION)"
export OPAL_ROCK_VERSION="$(read_env OPAL_ROCK_VERSION)"
export OPAL_LOCAL_PASS="$(read_env OPAL_LOCAL_PASS)"
echo "== Opal :$OPAL_LOCAL_PORT (opal_citest:${OPAL_VERSION:-latest} + rock-base:${OPAL_ROCK_VERSION:-latest}) =="
docker compose -f "$COMPOSE" up -d
wait_for "Opal" "$OPAL_URL"

# --- Armadillo (background) -------------------------------------------------
mkdir -p "$ARMA_DIR"
echo "== Armadillo (background; log: $ARMA_LOG) =="
bash "$BENCH/run_local_armadillo.sh" > "$ARMA_LOG" 2>&1 &
echo $! > "$ARMA_PID"
wait_for "Armadillo" "$ARMA_URL/actuator/health"

echo
echo "Local servers ready. Run:"
echo "  bash run_benchmark.sh setup.R measure.R plots.R"
echo "Stop with: bash stop_servers.sh"
