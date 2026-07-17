# Flower e2e migration — change notes

Branch: `test/flower-release-test`. Companion package: `molgenis-flwr-armadillo`
(`feat/api-helpers` = src/CLIs, `docs/example-app` = the Hub app, merged into
`feat/api-helpers`).

## Goal

Stop using monkey-patched Flower Docker images. Run the e2e test with **stock
Flower 1.32.1** images and apps pulled **only from Flower Hub**, so app trust
comes from Hub publishing + reviewer signatures rather than a homegrown
consortium-signing scheme.

## Why the patched images are gone

- `timmyjc/verified-supernode`, `verified-superlink`, `superexec-data-test`
  are replaced by stock `flwr/superlink|supernode|superexec:1.32.1`.
- FAB signature verification (`--trusted-entities`) is a **stock** supernode
  feature, not a patch. Stock supernodes reject any FAB without a signature
  from a trusted reviewer key — including FABs pushed directly to the
  SuperLink (those carry no verifications). So "Hub-only apps" is the only
  stock-supported trust path.
- The old superexec patches are obsolete: ServerApp log suppression is now
  by-design (use `flwr run --stream`); baked-in deps are replaced by
  `--allow-runtime-dependency-installation` (1.29+).

## Trust model (production)

- **App trust:** app published to Flower Hub (`flwr app publish`), reviewed and
  signed (`flwr app review`). Supernodes run with `--trusted-entities` listing
  the reviewer public keys the node operator trusts.
- **Node identity:** SuperLink runs with `--enable-supernode-auth`; each
  supernode presents an EC key (Armadillo passes `--auth-supernode-private-key`,
  mandatory in the backend). Node public keys are registered with the SuperLink
  (`flwr supernode register`). In the test this registration is scaffolding, not
  an asserted scenario — the SuperLink is ops-owned, not Armadillo.

## Backend changes (armadillo repo, via feat/flower-containers + container-data-endpoint)

- `DockerService`: injects `ARMADILLO_URL` (from `flower.armadillo-url`) into
  flower-superexec containers, alongside `ARMADILLO_CONTAINER_NAME`.
- `DockerService`: always appends supernode security args
  (`--root-certificates`, `--auth-supernode-private-key`, `--trusted-entities`,
  `--clientappio-api-address`, `--isolation process`) and mounts the cert /
  auth-key / trusted-entities files; refuses to start if they are missing.
- Push endpoint (`/flower/push-data`, `FlowerController` / `FlowerDataService`)
  unchanged in intent; the app loads data through it.

## Package changes (molgenis-flwr-armadillo)

- **Token routing redesigned to a single-key blob.** Flower validates
  `--run-config` keys against the app's declared config, and a published Hub
  app can't declare per-node URL keys (unknown at publish time). So all tokens
  now travel in ONE declared key, `armadillo-tokens`, as base64(JSON
  {sanitized-url: token}). `run.py` builds the blob; `extract_tokens` forwards
  it; `get_node_token` decodes it and picks this node's token by sanitized
  `ARMADILLO_URL`. Supersedes the earlier URL-keyed run-config design.
- Helpers vendored into the app (`pytorchexample/armadillo.py`) because the Hub
  builds the FAB server-side and the package isn't on PyPI.

## The Hub app (examples/pytorch-armadillo)

- Renamed to `quickstart-pytorch-armadillo`, `publisher = "timmyjc"`,
  `fab-format-version = 1`, `flwr-version-target = "1.32.1"`,
  `license = { file = "LICENSE" }` + Apache-2.0 LICENSE.
- Declares `armadillo-tokens = ""` in `[tool.flwr.app.config]`.
- Simulation-mode branch added (partitions CIFAR-10 when partition-id /
  num-partitions are in node_config), so it runs in both runtimes.

## Test scripts (scripts/release/flower/)

- `config.sh`: stock 1.32.1 images; `generate_flower_credentials` creates the
  TLS cert (RSA/SHA-256 — LibreSSL/gRPC won't accept EC or SHA-1 here) plus a
  per-node EC (`ecdsa -b 256`) supernode auth key and trusted-entities.yaml
  from `.env` reviewer key; `register_supernode_keys` registers node keys with
  the SuperLink; `write_flwr_cli_config` writes a `local` TLS connection;
  `run_config_from_tokens` builds the `armadillo-tokens` blob.
- `start-superlink.sh`: TLS SuperLink with `--enable-supernode-auth`; serverapp
  superexec connects `--insecure` (ServerAppIo/9091 is plaintext) with runtime
  dep install.
- `start-armadillos.sh`: passes `--flower.armadillo-url` per instance.
- `register-flower-containers.sh`: stock images; supernode configs use
  `trustedEntitiesPath`/`caCertPath`/`authPrivateKeyPath`; Armadillo appends the
  security args.
- Scenarios A–F rewritten for the Hub flow (`flwr run @account/app`): A correct
  tokens, B wrong token, C wrong project, D non-Hub app rejected, E no tokens,
  F Hub app signed by untrusted reviewer rejected.
- `cleanup.sh`: no longer deletes `flower-nodes.yaml` (user config).
- `flower-nodes.yaml`: `urls:` list; uses a host-reachable address so host and
  containers agree on the URL for token routing (LAN IP when
  host.docker.internal can't be added to /etc/hosts).

## Setup gotchas found while getting it green

- TLS cert must be RSA + SHA-256 (macOS LibreSSL signs SHA-1; gRPC rejects both
  SHA-1 and its EC key output).
- Supernode auth key must be EC (`ecdsa -b 256`), not ed25519 (Exit 302).
- SuperLink node auth: `--auth-list-public-keys` is removed; must use
  `--enable-supernode-auth` + `flwr supernode register`.
- ServerAppIo (9091) is plaintext unless `--appio-ssl-*` is set → serverapp
  connects `--insecure`.
- `flwr` CLI can't use passphrase-protected signing keys (loads with
  password=None) — use a no-passphrase Ed25519 key for `flwr app review`.
