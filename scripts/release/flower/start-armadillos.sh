#!/usr/bin/env bash
#
# Start two Armadillo instances with OIDC, wait for both to be ready,
# and verify OIDC is configured. Writes PIDs to $PID_FILE.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f "$ARMADILLO_JAR" ] || fail "Armadillo JAR not found at $ARMADILLO_JAR. Run: ./gradlew bootJar"

log "Starting Armadillo instance 1 (port $ARMADILLO_1_PORT) ..."
mkdir -p "$ARMADILLO_1_DATA"
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

log "Starting Armadillo instance 2 (port $ARMADILLO_2_PORT) ..."
mkdir -p "$ARMADILLO_2_DATA"
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

echo "$ARMADILLO_1_PID $ARMADILLO_2_PID" > "$PID_FILE"

wait_for_armadillo $ARMADILLO_1_PORT
wait_for_armadillo $ARMADILLO_2_PORT
