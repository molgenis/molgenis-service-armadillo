#!/usr/bin/env zsh

OUT=/dev/stdout
if [ -z "$1" ]; then 
    OUT="/dev/null"
fi

function usage() {
    echo 'zsh -c "source dev.env ; ./test_armadilloctl.zsh" || echo FAILED'
}

function colors() {
  for c in {0..255}; do
    printf "\033[48;5;%sm%3d\033[0m " "$c" "$c"
    if (( c == 15 )) || (( c > 15 )) && (( (c-15) % 6 == 0 )); then
      printf "\n"
    fi
  done
}

OK="\033[32m"
WARN="\033[33m"
ERR="\033[31m"
B="\033[0m"

function error() {
  echo -e "TEST:: ERROR: ${ERR}${1}${B}"
}

function warning() {
  echo -e "TEST:: WARNING: ${WARN}${1}${B}"
}

function success() {
  echo -e "TEST:: SUCCES: ${OK}${1}${B}"
}


function failed() {
    warn $0
}
function do_sleep() {
    warning "Pausing for $1 seconds before '$2'."
    sleep "$1"
    echo "..."
}

error "NOTE: controller exit values are incomplete ... needs eyes"
warning "Make sure Armadillo is running"

./armadilloctl.zsh > $OUT || success "Needs a command."

./armadilloctl.zsh status > $OUT || success "status needs profile name"
./armadilloctl.zsh statusAll > $OUT && success "statusAll needs running Armadillo"

./armadilloctl.zsh stopAll > $OUT && success "stopAll needs running Armadillo"

do_sleep 5 "autoStart"
./armadilloctl.zsh autoStart > $OUT && success "autoStart needs running Armadillo"

do_sleep 5 "startAll"
./armadilloctl.zsh startAll > $OUT && success "startAll needs running Armadillo"

do_sleep 5 "restartAll"
./armadilloctl.zsh restartAll > $OUT && success "restartAll needs running Armadillo"

# colors
