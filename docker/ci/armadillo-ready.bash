#!/usr/bin/env bash

# Default duration is 60 if not supplied
duration=${1:-60}
HOSTNAME=${2:-armadillo}

for ((i=1; i<=duration; i++))
do
  echo "$i: is Armadillo up?"
  if wget --spider "http://${HOSTNAME}:8080"; then
    echo "Armadillo is up according to wget ..."
    ./release-test.R || exit 1
    exit 0
  else
    echo "Armadillo still down ..."
    sleep 2
  fi
done
exit 1

