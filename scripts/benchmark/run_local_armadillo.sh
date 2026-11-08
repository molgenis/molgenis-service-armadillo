#!/usr/bin/env bash
# ==============================================================================
# Download and run a specific RELEASED Armadillo locally on :8080, so a local
# backend can be pinned to a chosen version (to compare releases). Runs in the
# foreground with the same 2 GB heap cap as Opal (memory parity) -- Ctrl-C stops.
#
#   bash run_local_armadillo.sh            # version + creds from .env
#   ARMADILLO_VERSION=5.12.2 bash run_local_armadillo.sh   # or override inline
#
# Requires Java and Docker (Armadillo manages its Rock/Rserve profile containers).
# The jar is cached under .armadillo/; storage lives under .armadillo/storage.
# Basic auth comes from ARMA_LOCAL_USER/PASS in .env (default admin/admin). Create
# the default + rserve profiles once (see README "Armadillo profiles").
# ==============================================================================
set -euo pipefail

BENCH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Read one KEY from .env (inline ' # comment' + surrounding whitespace stripped),
# mirroring lib/config.R's parser. Env vars still win (checked before this).
read_env() {
  local k="$1" f="${ENV_FILE:-$BENCH/.env}"
  [ -f "$f" ] || return 0
  grep -E "^[[:space:]]*${k}=" "$f" | head -1 | sed -E "s/^[^=]*=//; s/[[:space:]]+#.*$//; s/[[:space:]]+$//"
}

VERSION="${ARMADILLO_VERSION:-$(read_env ARMADILLO_VERSION)}"
[ -n "$VERSION" ] || { echo "ARMADILLO_VERSION not set (in .env or the environment)" >&2; exit 1; }
USER_NAME="${ARMA_USER:-$(read_env ARMA_LOCAL_USER)}"; USER_NAME="${USER_NAME:-admin}"
PASS="${ARMA_PASS:-$(read_env ARMA_LOCAL_PASS)}";      PASS="${PASS:-admin}"
PORT="${ARMA_LOCAL_PORT:-$(read_env ARMA_LOCAL_PORT)}"; PORT="${PORT:-8080}"
JAR_DIR="$BENCH/.armadillo"
STORE="${ARMA_STORE:-$JAR_DIR/storage}"
JAR="$JAR_DIR/molgenis-armadillo-$VERSION.jar"
URL="https://github.com/molgenis/molgenis-service-armadillo/releases/download/v$VERSION/molgenis-armadillo-$VERSION.jar"

mkdir -p "$JAR_DIR" "$STORE"
if [ ! -f "$JAR" ]; then
  echo "Downloading Armadillo $VERSION ..."
  curl -fSL -o "$JAR" "$URL"
fi

echo "Starting Armadillo $VERSION on :$PORT (storage: $STORE)"
JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:--Xms1G -Xmx2G -XX:+UseG1GC}" \
  java -jar "$JAR" \
    --server.port="$PORT" \
    --spring.security.user.name="$USER_NAME" \
    --spring.security.user.password="$PASS" \
    --storage.root-dir="$STORE" \
    --armadillo.docker-management-enabled=true
