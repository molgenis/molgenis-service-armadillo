#!/usr/bin/env bash

CURL_OPTS=--silent

OK="\033[32m"
WARN="\033[33m"
ERR="\033[31m"
B="\033[0m"

success() {
  echo -e "${OK}${1}${B}"
}

warning() {
  echo -e "WARNING: ${WARN}${1}${B}"
}

error() {
  echo -e "ERROR: ${ERR}${1}${B}"
  exit 1
}

is_armadillo_running() {
  cmd="${ARMADILLO_URL}"
  curl $CURL_OPTS --request "GET" "${cmd}" > /dev/null || (error "Armadillo not running."; exit 1)
}

get_profiles() {
  cmd="$ARMADILLO_URL/ds-profiles"
  profile_names=$(curl $CURL_OPTS --user "${CREDENTIALS}" --request "GET" --header "accept: application/json" "${cmd}" | jq -r '.[] | "\(.name)"')
}

status() {
    if [ -z "$1" ]; then
        error "$0 needs profile name. Or try ${0}All"
    fi
    name=$1

    cmd="${ARMADILLO_URL}/ds-profiles/{$name}"
    stats=$(curl $CURL_OPTS --user "${CREDENTIALS}" --request "GET" --header "accept: application/json" "${cmd}" | jq -r '"\(.name) = \(.container.status)"')
    success "$stats"
}

start() {
    if [ -z "$1" ]; then
        error "$0 needs profile name. Or try ${0}All"
    fi

    container="$1"
    cmd="$ARMADILLO_URL/ds-profiles/$profile/start"
    echo "Starting '$profile' on $cmd"

    curl $CURL_OPTS --user $CREDENTIALS --request "POST" "$cmd" --data ""
}

stop() {
    if [ -z "$1" ]; then
        error "$0 needs profile name. Or try ${0}All"
    fi

    container="$1"
    cmd="$ARMADILLO_URL/ds-profiles/$profile/stop"
    echo "Stopping '$profile' on $cmd"

    curl $CURL_OPTS --user $CREDENTIALS --request "POST" $cmd --data ""
}

restart() {
    if [ -z "$1" ]; then
        error "$0 needs profile name. Or try ${0}All"
    fi

    container=$1
    echo "Restarting $profile"
    stop "$profile"
    sleep 5
    start "$profile"
    sleep 5
    status "$profile"

}

is_auto_start() {
  if [[ $ARMADILLO_PROFILES_AUTOSTART =~ (^|[[:space:]])$1($|[[:space:]]) ]]
  then
    return 0
  else
    return 1
  fi
}

doAll() {
  command=$1
  get_profiles

  echo "${profile_names}" | while read -r item; do
    "$command" "${item}"
  done
}

statusAll() {
  doAll status
}

startAll() {
    doAll start
}

stopAll() {
    doAll stop
}

restartAll() {
  doAll stop
  sleep 5
  doAll start
}

autoStart() {
    get_profiles
    echo "Auto starting ..."
    echo "${profile_names}" | while read -r item
    do
      if is_auto_start "${item}"
      then
        start "${item}"
      fi
    done
    statusAll
}

check_dependencies() {
  if ! which jq > /dev/null ; then
    echo "Please install jq for json parsing ... exiting"
    exit 1
  fi
}

var_found() {
  echo "Variable $1 found ..."
}

var_empty() {
  warning "Variable $1 not set! ... exiting"
  exit 1
}

all_set() {
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

  echo ""
  echo "Armadillo settings:"
  echo "  ARMADILLO_URL                : ${ARMADILLO_URL}"
  echo "  ARMADILLO_ADMIN_USER         : ${ARMADILLO_ADMIN_USER}"
  echo "  ARMADILLO_PROFILES_AUTOSTART : ${ARMADILLO_PROFILES_AUTOSTART}"
}

check_dependencies || exit

all_set || exit

is_armadillo_running || exit

get_profiles

if [[ "$1" =~ ^(status|start|stop|restart|statusAll|startAll|stopAll|restartAll|autoStart)$ ]]; then
    "$@"
else
  warning "Please provide one of the following arguments: status | start | stop | restart | statusAll | startAll | stopAll | restartAll | autoStart"
  error "Got argument '$1'"
fi
