# Benchmark

Opal-vs-Armadillo DataSHIELD speed benchmark across local and remote servers.
Config lives in one git-ignored `.env`; stages are R scripts in `lib/`.

## Setup (once)

```bash
./install_benchmark_dependencies.R   # R client + tooling into .Rlib
cp .env.dist .env                    
```
`.env.dist` documents every key. All backends use basic auth. Backends are picked
on two axes, listed once each: `BACKENDS` (engines: `opal`, `armadillo_rock`,
`armadillo_rserve`) and `LOCATIONS` (`local`, `remote`). Every engine runs at
every location.

The essential keys to fill in are:

OPAL_LOCAL_PORT=8081
OPAL_LOCAL_USER=administrator
OPAL_LOCAL_PASS=datashield_test&

OPAL_REMOTE_URL= [listed in vault]
OPAL_REMOTE_USER= [listed in vault]
OPAL_REMOTE_PASS= [listed in vault]

ARMA_LOCAL_PORT=8080
ARMA_LOCAL_USER=admin
ARMA_LOCAL_PASS=admin

ARMA_REMOTE_URL= [listed in vault]
ARMA_REMOTE_USER= [listed in vault]
ARMA_REMOTE_PASS= [listed in vault]

## Run

`BACKENDS` x `LOCATIONS` (in `.env`) pick which servers to benchmark.

### Run local

```
BACKENDS=opal,armadillo_rock,armadillo_rserve
LOCATIONS=local
```
```bash
./start_servers.sh                 # local Opal + Armadillo, version-pinned from .env
bash run_benchmark.sh setup.R measure.R plots.R
./stop_servers.sh                  # when done
```

### Run local + remote

Fill in the `*_REMOTE_*` keys, then add `remote` to the locations:

```
BACKENDS=opal,armadillo_rock,armadillo_rserve
LOCATIONS=local,remote
```

Run the same commands as above.

Profiles: the benchmark uses two Armadillo profiles, `default` (Rock) + `rserve`.
Locally, `setup.R` adds `rserve` (on RSERVE_IMAGE); `default` ships with Armadillo.
On REMOTE servers both must already exist, on the same pinned dsBase image, with
`dsBase` whitelisted and `privacyControlLevel = permissive`. Names come from
`ARMA_PROFILE` / `ARMA_RSERVE_PROFILE` / `OPAL_PROFILE`.

Stages (run any subset via `run_benchmark.sh <stage>...`):

| stage | does |
|---|---|
| `setup.R` | upload the vendored tables to every backend |
| `measure.R` | time functions + sessions → `results/speed_*.csv` (`CHECK=1` validates only) |
| `plots.R` | render all figures → `results/*.png` |

Skip `start_servers.sh`/`stop_servers.sh` if you only run remote backends.

## Notes

- **Data** is provided in (`data/tables.rda`, fixed 10k rows; rebuild with
  `make_data.R`) — no size knobs.
