#!/usr/bin/env bash
#
# Step 1: Start two Armadillo instances with OIDC + Docker management enabled.
#
# Saves PIDs to .armadillo-pids for use by cleanup.sh.
#
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

# --- Preflight ---------------------------------------------------------------

log "Checking prerequisites..."
[ -f "$ARMADILLO_JAR" ] || fail "Armadillo JAR not found at $ARMADILLO_JAR. Run: ./gradlew bootJar"
docker info >/dev/null 2>&1 || fail "Docker is not running."

# --- Start Armadillo instances -----------------------------------------------

mkdir -p "$ARMADILLO_1_DATA" "$ARMADILLO_2_DATA"

log "Starting Armadillo instance 1 (port $ARMADILLO_1_PORT) with OIDC..."
java -jar "$ARMADILLO_JAR" \
  --server.port=$ARMADILLO_1_PORT \
  --storage.root-dir="$ARMADILLO_1_DATA" \
  --spring.security.user.name=$ADMIN_USER \
  --spring.security.user.password=$ADMIN_PASS \
  --armadillo.docker-management-enabled=true \
  --spring.security.oauth2.client.provider.molgenis.issuer-uri="$OIDC_ISSUER_URI" \
  --spring.security.oauth2.client.registration.molgenis.client-id="$OIDC_CLIENT_ID" \
  --spring.security.oauth2.client.registration.molgenis.client-secret="$OIDC_CLIENT_SECRET" \
  --spring.security.oauth2.client.registration.molgenis.authorization-grant-type=authorization_code \
  --spring.security.oauth2.resourceserver.jwt.issuer-uri="$OIDC_ISSUER_URI" \
  --spring.security.oauth2.resourceserver.opaquetoken.client-id="$OIDC_CLIENT_ID" \
  > "$SCRIPT_DIR/armadillo1.log" 2>&1 &
ARMADILLO_1_PID=$!

log "Starting Armadillo instance 2 (port $ARMADILLO_2_PORT) with OIDC..."
java -jar "$ARMADILLO_JAR" \
  --server.port=$ARMADILLO_2_PORT \
  --storage.root-dir="$ARMADILLO_2_DATA" \
  --spring.security.user.name=$ADMIN_USER \
  --spring.security.user.password=$ADMIN_PASS \
  --armadillo.docker-management-enabled=true \
  --spring.security.oauth2.client.provider.molgenis.issuer-uri="$OIDC_ISSUER_URI" \
  --spring.security.oauth2.client.registration.molgenis.client-id="$OIDC_CLIENT_ID" \
  --spring.security.oauth2.client.registration.molgenis.client-secret="$OIDC_CLIENT_SECRET" \
  --spring.security.oauth2.client.registration.molgenis.authorization-grant-type=authorization_code \
  --spring.security.oauth2.resourceserver.jwt.issuer-uri="$OIDC_ISSUER_URI" \
  --spring.security.oauth2.resourceserver.opaquetoken.client-id="$OIDC_CLIENT_ID" \
  > "$SCRIPT_DIR/armadillo2.log" 2>&1 &
ARMADILLO_2_PID=$!

# Save PIDs for cleanup script
echo "$ARMADILLO_1_PID $ARMADILLO_2_PID" > "$PID_FILE"

# --- Wait for healthy --------------------------------------------------------

wait_for_armadillo $ARMADILLO_1_PORT
wait_for_armadillo $ARMADILLO_2_PORT

# --- Verify OIDC -------------------------------------------------------------

log "Verifying OIDC auth info..."
AUTH_INFO=$(curl -sf "$ARMADILLO_1_URL/actuator/info" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['auth']['issuerUri'])" 2>/dev/null || echo "")
if [ -z "$AUTH_INFO" ]; then
  fail "Armadillo /actuator/info did not return auth info. OIDC may not be configured correctly."
fi
log "OIDC configured: issuer=$AUTH_INFO"

log ""
log "Armadillo instances running (PIDs: $ARMADILLO_1_PID, $ARMADILLO_2_PID)"
log "Logs: $SCRIPT_DIR/armadillo1.log, $SCRIPT_DIR/armadillo2.log"
