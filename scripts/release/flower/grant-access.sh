#!/usr/bin/env bash
#
# Grant the researcher email access to the demo project on both Armadillos.
# Email and project name come from config.sh ($RESEARCHER_EMAIL, $PROJECT_NAME).
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

grant_access $ARMADILLO_1_PORT "$RESEARCHER_EMAIL" "$PROJECT_NAME"
grant_access $ARMADILLO_2_PORT "$RESEARCHER_EMAIL" "$PROJECT_NAME"
