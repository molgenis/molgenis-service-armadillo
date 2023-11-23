#!/usr/bin/env bash

# Default duration is 60 if not supplied
duration=${1:-60}

for ((i=1; i<=duration; i++))
do
  STATUS=`docker inspect --format '{{.State.Status}}' ci-cicd-1`
  echo "$i: r-cicd still running: $STATUS ?"

  if [ "$STATUS" == "running" ]; then
    echo "Still running"
    sleep 2
  else
    echo "Finished"
    #exit 0
  fi
done
#exit 1

