#!/usr/bin/env bash
# Stop the local benchmark servers (Opal containers + Armadillo jar).
# Thin wrapper for `start_servers.sh --down`.
exec bash "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/start_servers.sh" --down
