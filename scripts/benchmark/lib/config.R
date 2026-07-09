# ==============================================================================
# Shared configuration for the Opal-vs-Armadillo DataSHIELD benchmark.
#
# Config comes from .env (the single source of truth), loaded below. Do not edit
# URLs/credentials here -- set them in .env. Everything else is derived.
# ==============================================================================

# Run against the CRAN (Obiba) release of dsBaseClient from a project-local
# library, not whatever dev build is installed globally. Install once with:
#   install.packages("dsBaseClient", lib = ".Rlib",
#                    repos = "https://cran.obiba.org", dependencies = FALSE)
# (dependencies resolve from the system library). Override the path with BENCH_LIB.
# .env is the SINGLE SOURCE OF TRUTH for config (connection + run params). It is
# read here by the R scripts and never printed. Defaults to ../.env (scripts run
# from lib/); override the path with ENV_FILE. Parsed in R: no shell evaluation,
# values literal incl. special chars; inline ' # comments' are stripped.
local({
  env_file <- Sys.getenv("ENV_FILE", "../.env")
  if (!file.exists(env_file)) return(invisible())
  for (ln in readLines(env_file, warn = FALSE)) {
    if (grepl("^\\s*#", ln) || !grepl("=", ln)) next
    k <- trimws(sub("=.*$", "", ln))
    v <- trimws(sub("\\s+#.*$", "", sub("^[^=]*=", "", ln)))
    if (nzchar(k)) do.call(Sys.setenv, setNames(list(v), k))
  }
})

LOCAL_LIB <- Sys.getenv("BENCH_LIB", ".Rlib")
# BENCH_LIB may be a comma-separated list of libraries (first = highest priority),
# e.g. a patched-package lib followed by .Rlib. Existing dirs are prepended in order.
LOCAL_LIBS <- trimws(strsplit(LOCAL_LIB, ",")[[1]])
LOCAL_LIBS <- LOCAL_LIBS[dir.exists(LOCAL_LIBS)]
if (length(LOCAL_LIBS) > 0) {
  .libPaths(c(normalizePath(LOCAL_LIBS), .libPaths()))
} else {
  warning(sprintf(paste0("Project library '%s' not found - using the globally ",
    "installed dsBaseClient, whose version may not match the CRAN release this ",
    "benchmark targets. Install it with:\n  install.packages('dsBaseClient', ",
    "lib='%s', repos='https://cran.obiba.org', dependencies=FALSE)"),
    LOCAL_LIB, LOCAL_LIB), call. = FALSE)
}

suppressMessages({
  library(DSI)
  library(DSOpal)
  library(DSMolgenisArmadillo)
})

# --- Connection config (from .env) ------------------------------------------
# All connection values must be set explicitly (in .env). No silent defaults: a
# missing value fails loudly. Backends are named <kind>_<location>[_rserve]:
#   opal_local, opal_remote,
#   armadillo_local, armadillo_local_rserve,
#   armadillo_remote, armadillo_remote_rserve
# Each resolves its connection from a location-specific env prefix:
#   OPAL_LOCAL_* / OPAL_REMOTE_*   -> URL, USER, PASS
#   ARMA_LOCAL_* / ARMA_REMOTE_*   -> URL, USER, PASS, AUTH[, TOKEN]
require_env <- function(k) {
  v <- Sys.getenv(k)
  if (!nzchar(v)) stop(sprintf("%s is not set -- define it in your .env", k), call. = FALSE)
  v
}

# The two Armadillo compute profiles, benchmarked as separate backends per host.
ARMA_PROFILE        <- Sys.getenv("ARMA_PROFILE",        "default")
ARMA_RSERVE_PROFILE <- Sys.getenv("ARMA_RSERVE_PROFILE", "rserve")

# Path to a local Opal docker compose file; if set, bench.R restarts Opal on a
# mid-run crash. Leave blank to disable (reconnect-only).
OPAL_COMPOSE <- Sys.getenv("OPAL_COMPOSE", "")

backend_kind <- function(be) ifelse(grepl("^opal", be), "opal", "armadillo")
backend_location <- function(be) {
  if (grepl("_local", be)) {
    "local"
  } else if (grepl("_remote", be)) {
    "remote"
  } else {
    stop("backend '", be, "' must contain _local or _remote", call. = FALSE)
  }
}

# Resolve a backend name to its connection spec. Envs are required only for the
# backends actually used (this is called per requested backend).
backend_spec <- function(be) {
  loc <- backend_location(be)
  if (backend_kind(be) == "opal") {
    p <- sprintf("OPAL_%s_", toupper(loc))
    list(be = be, kind = "opal", driver = "OpalDriver",
         url = require_env(paste0(p, "URL")), user = require_env(paste0(p, "USER")),
         pass = require_env(paste0(p, "PASS")))
  } else {
    p    <- sprintf("ARMA_%s_", toupper(loc))
    auth <- tolower(Sys.getenv(paste0(p, "AUTH"), "basic"))
    list(be = be, kind = "armadillo", driver = "ArmadilloDriver",
         url = require_env(paste0(p, "URL")), user = require_env(paste0(p, "USER")),
         pass = if (auth == "basic") require_env(paste0(p, "PASS")) else Sys.getenv(paste0(p, "PASS"), ""),
         auth = auth, token = Sys.getenv(paste0(p, "TOKEN"), ""),
         profile = if (grepl("_rserve$", be)) ARMA_RSERVE_PROFILE else ARMA_PROFILE)
  }
}

# --- Data -------------------------------------------------------------------
# Benchmark data is REAL dsBaseClient test data, loaded from the package's
# tests/testthat/data_files and inflated to N_ROWS x ~N_VARS in setup.R. Each
# dataset becomes a table on both backends and is assigned to a fixed server
# symbol that the benchmark calls reference (D = CNSIM, DS = survival, etc.).
DATA_DIR <- Sys.getenv("DSBASECLIENT_DATA",
  file.path(Sys.getenv("HOME"), "git-repos/ds-core/dsBaseClient/tests/testthat/data_files"))

PROJECT <- "perf"
FOLDER  <- "bench"          # Armadillo folder (Opal has no folders)
N_ROWS  <- as.integer(Sys.getenv("N_ROWS", "100000"))
N_VARS  <- as.integer(Sys.getenv("N_VARS", "30"))   # target columns per table

# How a table name maps to a per-backend reference for datashield.assign.table(),
# by backend KIND: Opal uses project.table; Armadillo uses project/folder/table.
ds_table_ref <- function(be, tbl)
  if (backend_kind(be) == "opal") paste0(PROJECT, ".", tbl) else paste(PROJECT, FOLDER, tbl, sep = "/")

# Dataset registry (source of truth for setup.R upload + bench.R assigns):
#   rda     - path under DATA_DIR of the dsBaseClient .rda to load
#   table   - uploaded table name (same on both backends)
#   symbol  - server-side object the benchmark assigns the table to
#   kind    - row inflation strategy: "flat" samples rows with replacement;
#             "survival"/"cluster" tile + re-number id_cols so subject/grouping
#             identifiers stay valid and unique
#   id_cols - identifier columns re-numbered per tile (structured kinds)
#   slim    - keep only id/key + a few columns (the merge partner table)
DATASETS <- list(
  cnsim    = list(rda = "CNSIM/CNSIM1.rda", table = "CNSIM",   symbol = "D",  kind = "flat"),
  cnsim_b  = list(rda = "CNSIM/CNSIM1.rda", table = "CNSIM_B", symbol = "D2", kind = "flat", slim = TRUE),
  survival = list(rda = "SURVIVAL/EXPAND_NO_MISSING/EXPAND_NO_MISSING1.rda",
                  table = "SURVIVAL", symbol = "DS", kind = "survival", id_cols = c("id")),
  cluster  = list(rda = "CLUSTER/CLUSTER_SLO1.rda", table = "CLUSTER", symbol = "DC",
                  kind = "cluster", id_cols = c("idSurgery", "idDoctor"))
)

# All datasets are ALWAYS active -- every op has the tables it needs. No subsetting
# knob: the benchmark must always cover the full dataset set.

# Default login table (build_logins) + workspace save (setup.R). CNSIM is the
# default; the benchmark assigns every dataset explicitly regardless.
TABLE_A <- DATASETS$cnsim$table

# --- Benchmark settings -----------------------------------------------------
DURATION_SEC <- as.numeric(Sys.getenv("DURATION_SEC", "20"))  # seconds per cell
REPS         <- as.integer(Sys.getenv("REPS", "10"))          # repeats per cell
SEED         <- as.integer(Sys.getenv("SEED", "1"))           # shuffle seed
WORKSPACE    <- "perf_ws"                                     # saved in setup.R

# BACKENDS / output path / poll floor are env-overridable so one run can target a
# subset of backends, write to a separate CSV, and use a non-default DSI poll-sleep.
BACKENDS  <- trimws(strsplit(Sys.getenv("BACKENDS",
  "opal_local,opal_remote,armadillo_local,armadillo_local_rserve,armadillo_remote,armadillo_remote_rserve"),
  ",")[[1]])
OUT_CSV   <- Sys.getenv("OUT_CSV", file.path("results", "rates.csv"))

# DSI client poll-sleep floor (seconds). Default 50ms; lower it to reduce the
# client-side wait between "is it done?" checks (helps poll-dominated ops).
poll0 <- Sys.getenv("POLL_SLEEP0", "")
if (nzchar(poll0)) options(datashield.polling.sleep.0 = as.numeric(poll0))

# --- Per-backend helpers ----------------------------------------------------
# Per-backend reference to the default benchmark table (CNSIM).
table_a_ref <- function(be) ds_table_ref(be, TABLE_A)

# Armadillo OAuth token cache, keyed by host URL (only for non-basic auth).
# Fetched once, BEFORE any timed datashield.login, so the handshake isn't part of
# the measured login time (build_logins() does this at benchmark startup).
.arma_tokens <- new.env(parent = emptyenv())
arma_token <- function(url) {
  if (is.null(.arma_tokens[[url]]))
    .arma_tokens[[url]] <- MolgenisArmadillo::armadillo.get_token(url)
  .arma_tokens[[url]]
}

# Append one backend's login row from its spec (driver + basic/token auth +
# profile). The single place that knows the per-backend branching.
append_backend <- function(b, spec) {
  if (spec$kind == "opal") {
    b$append(server = spec$be, url = spec$url, user = spec$user, password = spec$pass,
             table = table_a_ref(spec$be), driver = "OpalDriver")
  } else if (spec$auth == "basic") {
    b$append(server = spec$be, url = spec$url, user = spec$user, password = spec$pass,
             table = table_a_ref(spec$be), driver = "ArmadilloDriver", profile = spec$profile)
  } else {
    tok <- if (nzchar(spec$token)) spec$token else arma_token(spec$url)
    b$append(server = spec$be, url = spec$url, token = tok,
             table = table_a_ref(spec$be), driver = "ArmadilloDriver", profile = spec$profile)
  }
}

# Build a multi-server logindata object over the requested backends; subset per
# backend with login_for().
build_logins <- function(backends = BACKENDS) {
  b <- DSI::newDSLoginBuilder(.silent = TRUE)
  for (be in backends) append_backend(b, backend_spec(be))
  b$build()
}

# A single-server logindata row for one backend.
login_for <- function(logindata, be) logindata[logindata$server == be, , drop = FALSE]
