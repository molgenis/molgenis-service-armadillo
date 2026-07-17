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
ARMADILLO_JAR="$(ls -t "$PROJECT_ROOT"/build/libs/molgenis-armadillo-*.jar 2>/dev/null | head -1)"
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

# --- Docker images (stock Flower; keep major.minor identical everywhere) -----

FLWR_VERSION="${FLWR_VERSION:-1.32.1}"
SUPERLINK_IMAGE="${SUPERLINK_IMAGE:-flwr/superlink:$FLWR_VERSION}"
SUPERNODE_IMAGE="${SUPERNODE_IMAGE:-flwr/supernode:$FLWR_VERSION}"
SUPEREXEC_IMAGE="${SUPEREXEC_IMAGE:-flwr/superexec:$FLWR_VERSION}"
FLWR_ARMADILLO_DIR="$PROJECT_ROOT/../molgenis-flwr-armadillo"

# --- Flower Hub ---------------------------------------------------------------

HUB_APP="${HUB_APP:-@timmyjc/quickstart-pytorch-armadillo}"
# Reviewer trust: key id assigned by Flower Hub (see the app page's
# Verifications section) and the matching Ed25519 OpenSSH public key file.
REVIEWER_KEY_ID="${REVIEWER_KEY_ID:-}"
REVIEWER_PUBLIC_KEY_FILE="${REVIEWER_PUBLIC_KEY_FILE:-}"

# --- Flower credentials (mounted into supernodes by Armadillo) ----------------

CERTS_DIR="$SCRIPT_DIR/certs"
ARMADILLO_1_FLOWER_DIR="$ARMADILLO_1_DATA/system/flower"
ARMADILLO_2_FLOWER_DIR="$ARMADILLO_2_DATA/system/flower"
# URL the superexec containers use to reach their Armadillo (requires
# "127.0.0.1 host.docker.internal" in /etc/hosts for local host-side use)
ARMADILLO_1_FLOWER_URL="${ARMADILLO_1_FLOWER_URL:-http://host.docker.internal:$ARMADILLO_1_PORT}"
ARMADILLO_2_FLOWER_URL="${ARMADILLO_2_FLOWER_URL:-http://host.docker.internal:$ARMADILLO_2_PORT}"

# --- Container names ---------------------------------------------------------

SUPERNODE_1="flower-supernode-1"
SUPERNODE_2="flower-supernode-2"
CLIENTAPP_1="flower-clientapp-1"
CLIENTAPP_2="flower-clientapp-2"
SUPERLINK="flower-test-superlink"
SERVERAPP="flower-test-serverapp"

# --- Flower app --------------------------------------------------------------

PROJECT_NAME="${PROJECT_NAME:-test-flower}"
FLWR_APP_DIR="${FLWR_APP_DIR:-$FLWR_ARMADILLO_DIR/examples/pytorch-armadillo}"
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

write_flwr_cli_config() {
  # Isolated Flower CLI config with a "local" connection to the TLS superlink.
  export FLWR_HOME="$SCRIPT_DIR/.flwr-home"
  mkdir -p "$FLWR_HOME"
  cat > "$FLWR_HOME/config.toml" <<EOF
[superlink.local]
address = "127.0.0.1:9093"
root-certificates = "$CERTS_DIR/ca.crt"
EOF
}

register_supernode_keys() {
  # SuperLink runs with --enable-supernode-auth, so each supernode's public
  # key must be registered before it can activate. Nodes retry activation, so
  # this can run after they start. Retries until the Control API is reachable.
  write_flwr_cli_config
  local pub i
  for pub in "$ARMADILLO_1_FLOWER_DIR/credentials.pub" "$ARMADILLO_2_FLOWER_DIR/credentials.pub"; do
    i=0
    until flwr supernode register "$pub" local >/dev/null 2>&1; do
      i=$((i + 1))
      [ $i -ge 30 ] && fail "Failed to register supernode key with SuperLink: $pub"
      sleep 1
    done
    log "Registered supernode key: $pub"
  done
}

run_config_from_tokens() {
  # Emit the single armadillo-tokens run-config key: base64(JSON
  # {sanitized-url: token}). Matches armadillo-flwr-run and the app's helpers.
  python3 - "$TOKEN_FILE" <<'PYEOF'
import base64, json, sys
tokens = json.load(open(sys.argv[1]))
mapping = {k[len("token-"):]: v for k, v in tokens.items() if k.startswith("token-")}
blob = base64.b64encode(json.dumps(mapping).encode()).decode()
print(f"armadillo-tokens='{blob}'")
PYEOF
}

generate_flower_credentials() {
  # TLS: one CA, one superlink server cert (SANs cover host + containers).
  # Supernode auth: one Ed25519 OpenSSH keypair per node.
  # Trust: trusted-entities.yaml listing the Hub reviewer's public key.
  [ -n "$REVIEWER_KEY_ID" ] || fail "REVIEWER_KEY_ID not set (see the app page's Verifications section on Flower Hub)"
  [ -f "$REVIEWER_PUBLIC_KEY_FILE" ] || fail "REVIEWER_PUBLIC_KEY_FILE not found: $REVIEWER_PUBLIC_KEY_FILE"

  mkdir -p "$CERTS_DIR" "$ARMADILLO_1_FLOWER_DIR" "$ARMADILLO_2_FLOWER_DIR"

  if [ ! -f "$CERTS_DIR/ca.crt" ]; then
    # Single self-signed cert used as both server cert and trust root.
    # LibreSSL (macOS) cannot SHA-256-sign via `x509 -req`, so no CSR chain.
    log "Generating SuperLink TLS certificate..."
    printf '[req]\ndistinguished_name=dn\nx509_extensions=v3\nprompt=no\n[dn]\nCN=flower-test-superlink\n[v3]\nsubjectAltName=DNS:localhost,DNS:host.docker.internal,DNS:%s,IP:127.0.0.1\n' "$SUPERLINK" \
      > "$CERTS_DIR/ssl.cnf"
    openssl req -x509 -sha256 -newkey rsa:2048 \
      -nodes -keyout "$CERTS_DIR/server.key" -out "$CERTS_DIR/server.crt" \
      -days 365 -config "$CERTS_DIR/ssl.cnf" >/dev/null 2>&1
    cp "$CERTS_DIR/server.crt" "$CERTS_DIR/ca.crt"
  fi

  for dir in "$ARMADILLO_1_FLOWER_DIR" "$ARMADILLO_2_FLOWER_DIR"; do
    if [ ! -s "$dir/credentials" ]; then
      log "Generating supernode auth key in $dir..."
      rm -f "$dir/credentials" "$dir/credentials.pub"
      # SuperNode node-auth requires an EC (SECP256R1) key, not ed25519.
      ssh-keygen -t ecdsa -b 256 -q -N "" -f "$dir/credentials"
    fi
    cp "$CERTS_DIR/ca.crt" "$dir/ca.crt"
    python3 - "$REVIEWER_KEY_ID" "$REVIEWER_PUBLIC_KEY_FILE" "$dir/trusted-entities.yaml" <<'PYEOF'
import sys
key_id, pub_file, out = sys.argv[1:4]
pub = open(pub_file).read().strip()
with open(out, "w") as f:
    f.write(f"{key_id}: {pub}\n")
PYEOF
  done
  log "Flower credentials ready."
}

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
