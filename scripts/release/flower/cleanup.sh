#!/usr/bin/env bash
#
# Cleanup: Stop Armadillo processes, remove Docker containers, clean up files.
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

log "Cleaning up..."

# --- Stop Armadillo processes ------------------------------------------------

if [ -f "$PID_FILE" ]; then
  read -r pids < "$PID_FILE"
  for pid in $pids; do
    if kill -0 "$pid" 2>/dev/null; then
      log "Stopping Armadillo (PID $pid)..."
      kill "$pid" 2>/dev/null || true
      wait "$pid" 2>/dev/null || true
    fi
  done
  rm -f "$PID_FILE"
fi

# Fallback: kill any Armadillo processes on our ports
for port in $ARMADILLO_1_PORT $ARMADILLO_2_PORT; do
  pid=$(lsof -ti "tcp:$port" 2>/dev/null || true)
  if [ -n "$pid" ]; then
    log "Killing process on port $port (PID $pid)..."
    kill "$pid" 2>/dev/null || true
  fi
done

# --- Remove Docker containers ------------------------------------------------

for c in $SERVERAPP $CLIENTAPP_1 $CLIENTAPP_2 $SUPERNODE_1 $SUPERNODE_2 $SUPERLINK; do
  if docker inspect "$c" >/dev/null 2>&1; then
    log "Removing container $c..."
    docker rm -f "$c" 2>/dev/null || true
  fi
done

# --- Remove data and generated files ----------------------------------------

rm -rf "$ARMADILLO_1_DATA" "$ARMADILLO_2_DATA"
rm -f "$SCRIPT_DIR/cifar10_train.pt" "$SCRIPT_DIR/cifar10_test.pt"
rm -f "$NODES_CONFIG"
rm -f "$SCRIPT_DIR/armadillo1.log" "$SCRIPT_DIR/armadillo2.log" "$SCRIPT_DIR/flwr-run.log"

log "Cleanup done."
