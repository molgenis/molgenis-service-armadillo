#!/usr/bin/env bash
# ==============================================================================
# Minimal resource snapshot of the LOCAL backends, on the same 2-core / 8 GiB
# setup as the latency benchmark. Memory + CPU per container, plus the Armadillo
# server JVM heap (it runs as a host process, so docker stats can't see it).
#
#   Run when the benchmark is IDLE  -> "resting" footprint
#   Run during a benchmark          -> "under load"
#
#   Opal      = opal-opal-1 (server) + opal-mongo-1 (Mongo) + opal-rock-1 (R engine)
#   Armadillo = server JVM (host) + default (Rock) + rserve (Rserve) containers
#
#   bash resources.sh
# ==============================================================================
set -uo pipefail

ARMA_URL="${ARMA_URL:-http://localhost:8081}"
ARMA_AUTH="${ARMA_AUTH:-admin:admin}"
OPAL_CTRS="opal-opal-1 opal-mongo-1 opal-rock-1"
ARMA_CTRS="default rserve"

echo "=== container memory / CPU  (docker stats --no-stream) ==="
printf '%-14s %-24s %s\n' NAME MEMORY CPU
docker stats --no-stream --format '{{.Name}}||{{.MemUsage}}||{{.CPUPerc}}' \
    $OPAL_CTRS $ARMA_CTRS 2>/dev/null \
  | awk -F'\\|\\|' '{ printf "%-14s %-24s %s\n", $1, $2, $3 }'

echo
echo "=== Armadillo server JVM heap (host process, via /actuator) ==="
mem=$(curl -s -u "$ARMA_AUTH" "$ARMA_URL/actuator/metrics/jvm.memory.used" \
        | grep -oE '"value":[0-9.eE+-]+' | head -1 | cut -d: -f2)
if [ -n "${mem:-}" ]; then
  awk -v m="$mem" 'BEGIN { printf "jvm.memory.used ~ %.0f MB\n", m/1048576 }'
else
  echo "  (couldn't read the actuator metric — check auth / that Armadillo is up)"
fi

echo
echo "=== install footprint  (docker image sizes) ==="
docker images --format '{{.Repository}}:{{.Tag}}||{{.Size}}' \
  | grep -iE 'opal|mongo|rock|rserve|armadillo' | sort -u \
  | awk -F'\\|\\|' '{ printf "%-46s %s\n", $1, $2 }'

echo
echo "=== data on disk  (load the SAME dataset into each first) ==="
# Armadillo stores tables as Parquet under its storage root; Opal keeps data in Mongo.
ARMA_STORE="${ARMA_STORE:-/Users/timcadman/git-repos/molgenis/molgenis-service-armadillo/data}"
if [ -d "$ARMA_STORE" ]; then
  du -sh "$ARMA_STORE" 2>/dev/null | awk '{ print "Armadillo parquet store: "$1"  ("$2")" }'
else
  echo "  Armadillo store not found at $ARMA_STORE (set ARMA_STORE=...)"
fi
docker exec opal-mongo-1 du -sh /data/db 2>/dev/null \
  | awk '{ print "Opal Mongo /data/db:     "$1 }' \
  || echo "  (couldn't read Opal Mongo data dir)"

echo
echo "note: Opal footprint = opal + mongo + rock; Armadillo = JVM heap + rock/rserve."
echo "      data efficiency is only comparable with the same dataset loaded in both."
