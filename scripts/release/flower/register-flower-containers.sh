#!/usr/bin/env bash
#
# Register the verified-supernode and clientapp container configs on both
# Armadillo nodes via PUT /containers. Does NOT start them.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

SUPERNODE_IMAGE="${SUPERNODE_IMAGE:-timmyjc/verified-supernode:test}"

log "Registering verified supernode + clientapp configs on Armadillo 1..."

put_container $ARMADILLO_1_PORT "$(cat <<EOF
{
  "type": "flower-supernode",
  "name": "$SUPERNODE_1",
  "image": "$SUPERNODE_IMAGE",
  "dockerOptions": {
    "volumes": {
      "/tmp/trusted-entities.yaml": "/app/trusted-entities.yaml"
    }
  },
  "dockerArgs": [
    "--trusted-entities", "/app/trusted-entities.yaml",
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=0 num-partitions=2 node-name='node1'",
    "--clientappio-api-address", "0.0.0.0:9094",
    "--isolation", "process"
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

log "Registering verified supernode + clientapp configs on Armadillo 2..."

put_container $ARMADILLO_2_PORT "$(cat <<EOF
{
  "type": "flower-supernode",
  "name": "$SUPERNODE_2",
  "image": "$SUPERNODE_IMAGE",
  "dockerOptions": {
    "volumes": {
      "/tmp/trusted-entities.yaml": "/app/trusted-entities.yaml"
    }
  },
  "dockerArgs": [
    "--trusted-entities", "/app/trusted-entities.yaml",
    "--insecure",
    "--superlink", "host.docker.internal:9092",
    "--node-config", "partition-id=1 num-partitions=2 node-name='node2'",
    "--clientappio-api-address", "0.0.0.0:9095",
    "--isolation", "process"
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
    "--appio-api-address", "$SUPERNODE_2:9095"
  ]
}
EOF
)"

log "Supernode and clientapp configs registered."
