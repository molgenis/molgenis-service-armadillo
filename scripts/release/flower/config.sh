#!/usr/bin/env bash
#
# Shared configuration for Flower data loading test scripts.
# Source this file from each script: source "$(dirname "${BASH_SOURCE[0]}")/config.sh"
#

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"

# --- Load .env if present ----------------------------------------------------

if [ -f "$SCRIPT_DIR/.env" ]; then
  set -a
  source "$SCRIPT_DIR/.env"
  set +a
fi

# --- Armadillo ---------------------------------------------------------------

ARMADILLO_1_PORT="${ARMADILLO_1_PORT:-8080}"
ARMADILLO_2_PORT="${ARMADILLO_2_PORT:-8081}"
ARMADILLO_JAR="$PROJECT_ROOT/build/libs/molgenis-armadillo-5.13.0-SNAPSHOT.jar"
ARMADILLO_1_DATA="$SCRIPT_DIR/data1"
ARMADILLO_2_DATA="$SCRIPT_DIR/data2"
ARMADILLO_1_URL="http://localhost:$ARMADILLO_1_PORT"
ARMADILLO_2_URL="http://localhost:$ARMADILLO_2_PORT"

ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASS="${ADMIN_PASS:-admin}"

# --- OIDC (auth.molgenis.org) ------------------------------------------------

OIDC_ISSUER_URI="${OIDC_ISSUER_URI:-https://auth.molgenis.org}"
OIDC_CLIENT_ID="${OIDC_CLIENT_ID:-b396233b-cdb2-449e-ac5c-a0d28b38f791}"
OIDC_CLIENT_SECRET="${OIDC_CLIENT_SECRET:-nRO_t1_cIpdzRzr-cWBeZg4ckBsMHmX2MlA9SaSg4P8}"
RESEARCHER_EMAIL="${RESEARCHER_EMAIL:-t.j.cadman@umcg.nl}"

# --- Docker images -----------------------------------------------------------

SUPERLINK_IMAGE="${SUPERLINK_IMAGE:-flwr/superlink:1.23.0}"
SUPEREXEC_IMAGE="${SUPEREXEC_IMAGE:-timmyjc/superexec-data-test:0.0.1}"
FLWR_ARMADILLO_DIR="$PROJECT_ROOT/../molgenis-flwr-armadillo"

# --- Container names ---------------------------------------------------------

SUPERNODE_1="flower-supernode-1"
SUPERNODE_2="flower-supernode-2"
CLIENTAPP_1="flower-clientapp-1"
CLIENTAPP_2="flower-clientapp-2"
SUPERLINK="flower-test-superlink"
SERVERAPP="flower-test-serverapp"

# --- Flower app --------------------------------------------------------------

PROJECT_NAME="${PROJECT_NAME:-test-flower}"
FLWR_APP_DIR="${FLWR_APP_DIR:-$SCRIPT_DIR/quickstart-pytorch-data}"
TOKEN_FILE="$(python3 -c 'import tempfile; print(tempfile.gettempdir())')/flwr_tokens.json"
NODES_CONFIG="$SCRIPT_DIR/flower-nodes.yaml"
PID_FILE="$SCRIPT_DIR/.armadillo-pids"
VENV_DIR="$SCRIPT_DIR/.venv"

# --- Activate venv if available ----------------------------------------------

if [ -d "$VENV_DIR" ]; then
  source "$VENV_DIR/bin/activate"
fi

# --- Helpers -----------------------------------------------------------------

log()  { echo ">>> $*"; }
fail() { echo "FAIL: $*" >&2; exit 1; }

wait_for_armadillo() {
  local port=$1
  local max_wait=60
  local i=0
  while ! curl -sf "http://localhost:$port/actuator/health" >/dev/null 2>&1; do
    i=$((i + 1))
    if [ $i -ge $max_wait ]; then
      fail "Armadillo on port $port did not start within ${max_wait}s"
    fi
    sleep 1
  done
  log "Armadillo on port $port is ready."
}

put_container() {
  local port=$1
  local json=$2
  local response_file
  response_file=$(mktemp)
  local http_code
  http_code=$(curl -s -o "$response_file" -w "%{http_code}" \
    -u "$ADMIN_USER:$ADMIN_PASS" \
    -X PUT \
    -H "Content-Type: application/json" \
    -d "$json" \
    "http://localhost:$port/containers")
  if [ "$http_code" != "204" ] && [ "$http_code" != "200" ]; then
    echo "Response body: $(cat "$response_file")" >&2
    rm -f "$response_file"
    fail "PUT /containers on port $port returned HTTP $http_code"
  fi
  rm -f "$response_file"
}

start_container() {
  local port=$1
  local name=$2
  log "Starting container '$name' via Armadillo on port $port..."
  # Remove any existing Docker container with the same name
  docker rm -f "$name" 2>/dev/null || true
  local response_file
  response_file=$(mktemp)
  local http_code
  http_code=$(curl -s -o "$response_file" -w "%{http_code}" \
    -u "$ADMIN_USER:$ADMIN_PASS" \
    -X POST \
    "http://localhost:$port/containers/$name/start")
  if [ "$http_code" != "204" ] && [ "$http_code" != "200" ]; then
    echo "Response body: $(cat "$response_file")" >&2
    rm -f "$response_file"
    fail "POST /containers/$name/start on port $port returned HTTP $http_code"
  fi
  rm -f "$response_file"
}

wait_for_container_running() {
  local name=$1
  local max_wait=30
  local i=0
  while true; do
    local state
    state=$(docker inspect -f '{{.State.Status}}' "$name" 2>/dev/null || echo "missing")
    if [ "$state" = "running" ]; then
      log "Container '$name' is running."
      return 0
    fi
    i=$((i + 1))
    if [ $i -ge $max_wait ]; then
      fail "Container '$name' not running after ${max_wait}s (state: $state)"
    fi
    sleep 1
  done
}

create_project() {
  local port=$1
  local project=$2
  log "Creating project '$project' on port $port..."
  local response_file
  response_file=$(mktemp)
  local http_code
  http_code=$(curl -s -o "$response_file" -w "%{http_code}" \
    -u "$ADMIN_USER:$ADMIN_PASS" \
    -X PUT \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"$project\"}" \
    "http://localhost:$port/access/projects")
  if [ "$http_code" != "204" ] && [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
    echo "Response body: $(cat "$response_file")" >&2
    rm -f "$response_file"
    fail "Create project on port $port returned HTTP $http_code"
  fi
  rm -f "$response_file"
  log "Project '$project' created (HTTP $http_code)."
}

grant_access() {
  local port=$1
  local email=$2
  local project=$3
  log "Granting '$email' access to project '$project' on port $port..."
  local response_file
  response_file=$(mktemp)
  local http_code
  http_code=$(curl -s -o "$response_file" -w "%{http_code}" \
    -u "$ADMIN_USER:$ADMIN_PASS" \
    -X POST \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"$email\", \"project\": \"$project\"}" \
    "http://localhost:$port/access/permissions")
  if [ "$http_code" != "204" ] && [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
    echo "Response body: $(cat "$response_file")" >&2
    rm -f "$response_file"
    fail "Grant access on port $port returned HTTP $http_code"
  fi
  rm -f "$response_file"
  log "Access granted (HTTP $http_code)."
}

upload_to_storage() {
  local port=$1
  local project=$2
  local object_path=$3
  local file_path=$4
  log "Uploading $file_path to $project/$object_path on port $port..."
  local http_code
  http_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -u "$ADMIN_USER:$ADMIN_PASS" \
    -X POST \
    -F "file=@$file_path" \
    "http://localhost:$port/storage/projects/$project/objects" \
    -F "object=$object_path")
  if [ "$http_code" != "204" ] && [ "$http_code" != "200" ] && [ "$http_code" != "201" ]; then
    fail "Upload to storage on port $port returned HTTP $http_code"
  fi
  log "Upload complete."
}
