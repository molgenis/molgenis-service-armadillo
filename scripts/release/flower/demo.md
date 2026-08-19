# DATA MANAGER SETUP STEPS

## Step 1: Start the SuperLink

```bash
/Users/tcadman/github-repos/ds-molgenis/molgenis-service-armadillo/scripts/release/flower/start-superlink.sh
```

## Step 2: Start the Armadillo Servers

```bash
/Users/tcadman/github-repos/ds-molgenis/molgenis-service-armadillo/scripts/release/flower/start-armadillos.sh
```

## Step 3: Upload the Test Data

The CIFAR10 files were prepared during pre-setup. Now create the project on each Armadillo and upload the data.

```bash
/Users/tcadman/github-repos/ds-molgenis/molgenis-service-armadillo/scripts/release/flower/upload-data.sh
```

## Step 4: Grant Researcher Access

Grant the researcher email (`$RESEARCHER_EMAIL` from `config.sh`) access to the project on both Armadillos.

```bash
/Users/tcadman/github-repos/ds-molgenis/molgenis-service-armadillo/scripts/release/flower/grant-access.sh
```

## Step 5: Generate a keypair and trusted-entities.yaml

Generate a keypair for the Armadillo servers, and register the public key in `trusted-entities.yaml`. 

```bash
molgenis-flwr-keygen --name /tmp/consortium

python3 -c "
from pathlib import Path; import yaml
from cryptography.hazmat.primitives.serialization.ssh import load_ssh_public_key
from molgenis_flwr_armadillo.signing import derive_key_id
pub = Path('/tmp/consortium.pub').read_bytes()
kid = derive_key_id(load_ssh_public_key(pub))
Path('/tmp/trusted-entities.yaml').write_text(yaml.dump({kid: pub.decode()}, default_flow_style=False))
"
```

```bash
cat /tmp/trusted-entities.yaml
```

## Step 6: Start the Supernodes and Clientapps

Start the supernodes and clientapps via the Armadillo API, mounting 'trusted-entities.yaml':

```bash
/Users/tcadman/github-repos/ds-molgenis/molgenis-service-armadillo/scripts/release/flower/start-supernodes.sh
```

## Step 7: Sign the Flower App

```bash
molgenis-flwr-sign \
  --app-dir /Users/tcadman/github-repos/ds-molgenis/molgenis-service-armadillo/scripts/release/flower/quickstart-pytorch-data \
  --private-key /tmp/consortium.key \
  --output /tmp/study.sfab
```





# RESARCHER STEPS

## Step 1: Authenticate

Create the nodes config:

```bash
cat > /Users/tcadman/github-repos/ds-molgenis/molgenis-service-armadillo/scripts/release/flower/flower-nodes.yaml <<EOF
nodes:
  node1:
    url: http://localhost:8080
  node2:
    url: http://localhost:8081
EOF
```

Then authenticate:

```bash
molgenis-flwr-authenticate --config /Users/tcadman/github-repos/ds-molgenis/molgenis-service-armadillo/scripts/release/flower/flower-nodes.yaml
```

A browser opens for each node. Log in to get OIDC tokens.

## Step 2: Submit the Job

```bash
source /Users/tcadman/github-repos/ds-molgenis/molgenis-service-armadillo/scripts/release/flower/config.sh

TOKEN_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['token-node1'])")
TOKEN_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['token-node2'])")
URL_NODE1=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['url-node1'])")
URL_NODE2=$(python3 -c "import json; d=json.load(open('$TOKEN_FILE')); print(d['url-node2'])")

DOCKER_URL_NODE1="${URL_NODE1/localhost/host.docker.internal}"
DOCKER_URL_NODE2="${URL_NODE2/localhost/host.docker.internal}"
```

```bash
molgenis-flwr-run \
  --signed-fab /tmp/study.sfab \
  --federation-address 127.0.0.1:9093 \
  --stream \
  --run-config "token-node1='$TOKEN_NODE1' url-node1='$DOCKER_URL_NODE1' token-node2='$TOKEN_NODE2' url-node2='$DOCKER_URL_NODE2'"
```

The signed FAB passes supernode verification, the tokens authenticate with each Armadillo, data loads from storage, and training runs.