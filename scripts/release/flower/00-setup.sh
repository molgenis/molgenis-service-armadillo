#!/usr/bin/env bash
#
# Step 0: Set up Python virtual environment and install dependencies.
#
# Creates a venv and installs the Flower app (which pulls in torch,
# torchvision, flwr, and molgenis-flwr-armadillo).
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

VENV_DIR="$SCRIPT_DIR/.venv"

# --- Preflight ---------------------------------------------------------------

PYTHON="${PYTHON:-python3.9}"
command -v "$PYTHON" >/dev/null 2>&1 || fail "$PYTHON not found. Set PYTHON env var to your Python 3.9+ path."
command -v java >/dev/null 2>&1 || fail "java not found. Java 17+ is required."
[ -d "$FLWR_APP_DIR" ] || fail "Flower app directory not found at $FLWR_APP_DIR"

# --- Build Armadillo JAR -----------------------------------------------------

log "Building Armadillo JAR..."
(cd "$PROJECT_ROOT" && ./gradlew clean bootJar)
[ -f "$ARMADILLO_JAR" ] || fail "JAR not found at $ARMADILLO_JAR after build"
log "JAR built: $ARMADILLO_JAR"

# --- Create venv (recreate if it exists) -------------------------------------

if [ -d "$VENV_DIR" ]; then
  log "Removing existing virtual environment..."
  rm -rf "$VENV_DIR"
fi

log "Creating virtual environment with $PYTHON..."
"$PYTHON" -m venv "$VENV_DIR"
source "$VENV_DIR/bin/activate"

# --- Install dependencies ----------------------------------------------------

log "Installing dependencies..."
pip install --upgrade pip
pip install "$FLWR_APP_DIR"

log ""
log "Setup complete. Activate the venv before running other scripts:"
log "  source $VENV_DIR/bin/activate"
