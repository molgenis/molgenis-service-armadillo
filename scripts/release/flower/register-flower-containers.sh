#!/usr/bin/env bash
#
# Register the supernode and clientapp container configs on both
# Armadillo nodes via PUT /containers. Does NOT start them.
#
# Armadillo auto-appends the supernode security args (--trusted-entities,
# --root-certificates, --auth-supernode-private-key, --clientappio-api-address,
# --isolation process) and mounts the files given by trustedEntitiesPath,
# caCertPath and authPrivateKeyPath. Run generate_flower_credentials first.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

log "Registering supernode + clientapp configs on Armadillo 1..."

put_container $ARMADILLO_1_PORT "$(cat <<EOF
{
  "type": "flower-supernode",
  "name": "$SUPERNODE_1",
  "image": "$SUPERNODE_IMAGE",
  "trustedEntitiesPath": "$ARMADILLO_1_FLOWER_DIR/trusted-entities.yaml",
  "caCertPath": "$ARMADILLO_1_FLOWER_DIR/ca.crt",
  "authPrivateKeyPath": "$ARMADILLO_1_FLOWER_DIR/credentials",
  "dockerArgs": [
    "--superlink", "host.docker.internal:9092"
  ]
}
EOF
)"

put_container $ARMADILLO_1_PORT "$(cat <<EOF
{
  "type": "flower-superexec",
  "name": "$CLIENTAPP_1",
  "image": "$SUPEREXEC_IMAGE",
  "dockerArgs": [
    "--insecure",
    "--plugin-type", "clientapp",
    "--appio-api-address", "$SUPERNODE_1:9094"
  ]
}
EOF
)"

log "Registering supernode + clientapp configs on Armadillo 2..."

put_container $ARMADILLO_2_PORT "$(cat <<EOF
{
  "type": "flower-supernode",
  "name": "$SUPERNODE_2",
  "image": "$SUPERNODE_IMAGE",
  "trustedEntitiesPath": "$ARMADILLO_2_FLOWER_DIR/trusted-entities.yaml",
  "caCertPath": "$ARMADILLO_2_FLOWER_DIR/ca.crt",
  "authPrivateKeyPath": "$ARMADILLO_2_FLOWER_DIR/credentials",
  "dockerArgs": [
    "--superlink", "host.docker.internal:9092"
  ]
}
EOF
)"

put_container $ARMADILLO_2_PORT "$(cat <<EOF
{
  "type": "flower-superexec",
  "name": "$CLIENTAPP_2",
  "image": "$SUPEREXEC_IMAGE",
  "dockerArgs": [
    "--insecure",
    "--plugin-type", "clientapp",
    "--appio-api-address", "$SUPERNODE_2:9094"
  ]
}
EOF
)"

log "Supernode and clientapp configs registered."
