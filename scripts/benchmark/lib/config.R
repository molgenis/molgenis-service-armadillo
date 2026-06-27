# ==============================================================================
# Shared configuration for the Opal-vs-Armadillo DataSHIELD benchmark.
#
# This is the ONLY file you edit to move from localhost to external servers:
# change the URLs / credentials below. Everything else is derived.
# ==============================================================================

# Run against the CRAN (Obiba) release of dsBaseClient from a project-local
# library, not whatever dev build is installed globally. Install once with:
#   install.packages("dsBaseClient", lib = ".Rlib",
#                    repos = "https://cran.obiba.org", dependencies = FALSE)
# (dependencies resolve from the system library). Override the path with BENCH_LIB.
# Optional: load credentials/URL from an env file (ENV_FILE=path), e.g. for a
# remote server. Parsed in R (no shell evaluation, values literal incl. special
# chars, never echoed); inline ' # comments' are stripped. Only active when set,
# so localhost runs are unaffected.
if (nzchar(Sys.getenv("ENV_FILE"))) local({
  for (ln in readLines(Sys.getenv("ENV_FILE"), warn = FALSE)) {
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

# --- Opal -------------------------------------------------------------------
OPAL_URL  <- Sys.getenv("OPAL_URL",  "http://localhost:8080")
OPAL_USER <- Sys.getenv("OPAL_USER", "administrator")
OPAL_PASS <- Sys.getenv("OPAL_PASS", "datashield_test&")
# Path to Opal's docker compose file. If set, bench.R auto-restarts Opal (which
# crashes intermittently) when its connection drops mid-run, so long unattended
# runs survive. Leave blank to disable auto-restart (reconnect-only).
OPAL_COMPOSE <- Sys.getenv("OPAL_COMPOSE", "")

# --- Armadillo --------------------------------------------------------------
# Auth is token-based by default: if ARMA_TOKEN is unset, arma_token() fetches one
# once via armadillo.get_token() (OAuth), OUTSIDE the timed login step. Set
# ARMA_AUTH=basic to use user/password instead (e.g. a local dev Armadillo).
ARMA_URL   <- Sys.getenv("ARMA_URL",   "http://localhost:8081")
ARMA_USER  <- Sys.getenv("ARMA_USER",  "admin")
ARMA_PASS  <- Sys.getenv("ARMA_PASS",  "admin")
ARMA_TOKEN <- Sys.getenv("ARMA_TOKEN", "")   # blank => fetched via armadillo.get_token()

# DataSHIELD compute profiles on the SAME Armadillo server/data: the default
# profile and the Rserve profile are benchmarked as two separate backends.
ARMA_PROFILE        <- Sys.getenv("ARMA_PROFILE",        "default")
ARMA_RSERVE_PROFILE <- Sys.getenv("ARMA_RSERVE_PROFILE", "rserve")

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

# How a table name maps to a per-backend reference for datashield.assign.table().
ds_table_ref <- function(be, tbl)
  if (be == "opal") paste0(PROJECT, ".", tbl) else paste(PROJECT, FOLDER, tbl, sep = "/")

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

# Restrict the active datasets via env (comma-separated keys), e.g. BENCH_DATASETS=cnsim
# to upload/assign only CNSIM. Lets a run (esp. remote) work off a single table.
.use_ds <- trimws(strsplit(Sys.getenv("BENCH_DATASETS", ""), ",")[[1]])
.use_ds <- .use_ds[nzchar(.use_ds)]
if (length(.use_ds) > 0) DATASETS <- DATASETS[.use_ds]

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
BACKENDS  <- trimws(strsplit(Sys.getenv("BACKENDS", "opal,armadillo,armadillo_rserve"), ",")[[1]])
OUT_CSV   <- Sys.getenv("OUT_CSV", file.path("results", "rates.csv"))

# DSI client poll-sleep floor (seconds). Default 50ms; lower it to reduce the
# client-side wait between "is it done?" checks (helps poll-dominated ops).
poll0 <- Sys.getenv("POLL_SLEEP0", "")
if (nzchar(poll0)) options(datashield.polling.sleep.0 = as.numeric(poll0))

# --- Per-backend helpers ----------------------------------------------------
# Per-backend reference to the default benchmark table (CNSIM).
table_a_ref <- function(be) ds_table_ref(be, TABLE_A)

# Fetch the Armadillo OAuth token once and cache it in ARMA_TOKEN. Call this
# BEFORE any timed datashield.login so the handshake is not part of the measured
# login time (build_logins() below does exactly that, at benchmark startup).
arma_token <- function() {
  if (!nzchar(ARMA_TOKEN))
    ARMA_TOKEN <<- MolgenisArmadillo::armadillo.get_token(ARMA_URL)
  ARMA_TOKEN
}

# Append one Opal / one Armadillo login row. These two are the single place that
# knows the driver + token-vs-basic-auth + profile branching (arma_token() is
# cached, fetched once, outside any timed login). basic auth when ARMA_AUTH=basic.
arma_basic  <- function() identical(tolower(Sys.getenv("ARMA_AUTH", "token")), "basic")

opal_append <- function(b, server, table)
  b$append(server = server, url = OPAL_URL, user = OPAL_USER, password = OPAL_PASS,
           table = table, driver = "OpalDriver")

arma_append <- function(b, server, table, profile) {
  if (arma_basic())
    b$append(server = server, url = ARMA_URL, user = ARMA_USER, password = ARMA_PASS,
             table = table, driver = "ArmadilloDriver", profile = profile)
  else
    b$append(server = server, url = ARMA_URL, token = arma_token(),
             table = table, driver = "ArmadilloDriver", profile = profile)
}

# Build a multi-server logindata object; subset per backend with login_for().
# Both Armadillo backends point at the same server/data and differ only by
# profile (default vs rserve).
build_logins <- function() {
  b <- DSI::newDSLoginBuilder(.silent = TRUE)
  opal_append(b, "opal",             table_a_ref("opal"))
  arma_append(b, "armadillo",        table_a_ref("armadillo"),        ARMA_PROFILE)
  arma_append(b, "armadillo_rserve", table_a_ref("armadillo_rserve"), ARMA_RSERVE_PROFILE)
  b$build()
}

# A single-server logindata row for one backend.
login_for <- function(logindata, be) logindata[logindata$server == be, , drop = FALSE]
