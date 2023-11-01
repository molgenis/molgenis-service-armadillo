#!/usr/bin/env zsh

CURL_OPTS=--silent

# curl -X 'GET' \
#   'http://localhost:8080/ds-profiles' \
#   -H 'accept: application/json' \
#   -H 'Authorization: Basic YWRtaW46YWRtaW4='

function get_profiles() {
  profile_names=$(curl $CURL_OPTS --user $CREDENTIALS --request 'GET' --header 'accept: application/json' $ARMADILLO_URL/ds-profiles | jq -r '.[] | "\(.name)"')
}

function status {
    if [ -z "$1" ]; then
        echo "status needs profile name."
        return 1
    fi
    name=$1

    stats=$(curl $CURL_OPTS --user "${CREDENTIALS}" --request 'GET' --header 'accept: application/json' "${ARMADILLO_URL}/ds-profiles/{$name}" | jq -r '"\(.name) = \(.container.status)"')
    echo $stats
}

function start {
    if [ -z "$1" ]; then
        echo "start needs profile name."
        return 1
    fi

    profile=$1
    cmd="$ARMADILLO_URL/ds-profiles/$profile/start"
    echo "Starting '$profile' on $cmd"

    curl --user $CREDENTIALS --request 'POST' $cmd --data ''
}

function stop {
    if [ -z "$1" ]; then
        echo "stop needs profile name."
        return 1
    fi

    profile=$1
    cmd="$ARMADILLO_URL/ds-profiles/$profile/stop"
    echo "Stopping '$profile' on $cmd"

    curl --user $CREDENTIALS --request 'POST' $cmd --data ''
}

function restart {
    if [ -z "$1" ]; then
        echo "restart needs profile name."
        return 1
    fi

    profile=$1
    echo "Restarting $profile"
    stop "$profile"
    sleep 5
    start "$profile"
    sleep 5
    status "$profile"

}

function is_auto_start() {
  if [[ $ARMADILLO_PROFILES_AUTOSTART =~ (^|[[:space:]])$1($|[[:space:]]) ]]
  then
    return 0
  else
    return 1
  fi
}

function doAll {
  command=$1
  get_profiles

  echo "${profile_names}" | while read -r item; do
    "$command" "${item}"
  done
}

function statusAll() {
  doAll status
}

function startAll() {
    doAll start
}

function stopAll() {
    doAll stop
}

function restartAll {
  doAll stop
  sleep 5
  doAll start
}

function autoStart() {
    get_profiles
    echo "Auto starting ..."
    echo "${profile_names}" | while read -r item
    do
      if is_auto_start "${item}"
      then
        start "${item}"
      fi
    done
}

function check_dependencies() {
  if ! which jq > /dev/null ; then
    echo "Please install jq for json parsing ... exiting"
    exit 1
  fi
}

function var_found() {
  echo "Variable $1 found"
}

function var_empty() {
  echo "Variable $1 not set! ... exiting"
  exit 1
}

function all_set() {
  [[ -n "$ARMADILLO_URL" ]] && var_found ARMADILLO_URL
  [[ -z "$ARMADILLO_URL" ]] && var_empty ARMADILLO_URL

  [[ -n "$ARMADILLO_ADMIN_USER" ]] && var_found ARMADILLO_ADMIN_USER
  [[ -z "$ARMADILLO_ADMIN_USER" ]] && var_empty ARMADILLO_ADMIN_USER

  [[ -n "$ARMADILLO_ADMIN_PASSWORD" ]] && var_found ARMADILLO_ADMIN_PASSWORD
  [[ -z "$ARMADILLO_ADMIN_PASSWORD" ]] && var_empty ARMADILLO_ADMIN_PASSWORD

  # Set to all if not set with value.
  [[ -n "${ARMADILLO_PROFILES_AUTOSTART}" ]] && var_found ARMADILLO_PROFILES_AUTOSTART
  [[ -z "${ARMADILLO_PROFILES_AUTOSTART}" ]] && ARMADILLO_PROFILES_AUTOSTART="__ALL__"

  CREDENTIALS="${ARMADILLO_ADMIN_USER}:${ARMADILLO_ADMIN_PASSWORD}"

  echo "Armadillo settings:"
  echo "  URL                : ${ARMADILLO_URL}"
  echo "  ADMIN_USER         : ${ARMADILLO_ADMIN_USER}"
  echo "  PROFILES_AUTOSTART : ${ARMADILLO_PROFILES_AUTOSTART}"
}

check_dependencies || exit

# FIXME: loads from local files
if [[ -f ".env" ]]
then
  echo "Sourcing .env"
  source ".env"
else
  echo "Sourcing dev.env"
  source "dev.env"
fi

all_set

get_profiles

if [[ "$1" =~ ^(status|start|stop|restart|statusAll|startAll|stopAll|restartAll|autoStart)$ ]]; then
    "$@"
else
    echo "Please provide one of the following argument: status | start | stop | restart | statusAll | startAll | stopAll | restartAll | autoStart"
fi
