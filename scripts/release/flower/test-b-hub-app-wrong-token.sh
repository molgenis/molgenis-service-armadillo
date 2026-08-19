#!/usr/bin/env bash
#
# Scenario B: Hub app + wrong token (should fail auth)
#
# The app's hash is whitelisted so the SuperExec launches it, but Armadillo
# rejects the invalid token on data load.
#
set -euo pipefail

source "$(dirname "${BASH_SOURCE[0]}")/config.sh"

[ -f "$TOKEN_FILE" ] || fail "Token file not found at $TOKEN_FILE. Run armadillo-flwr-authenticate first."

write_flwr_cli_config

RUN_CONFIG="$(python3 - "$TOKEN_FILE" <<'PYEOF'
import base64, json, sys
tokens = json.load(open(sys.argv[1]))
mapping = {k[len("token-"):]: "invalid-token" for k in tokens if k.startswith("token-")}
blob = base64.b64encode(json.dumps(mapping).encode()).decode()
print(f"armadillo-tokens='{blob}'")
PYEOF
)"

log "Scenario B: Hub app + wrong token"
log "Expected: data loading fails with HTTP 401 from Armadillo"
log ""

flwr run "$HUB_APP==$HUB_APP_VERSION" local --stream --run-config "$RUN_CONFIG"
