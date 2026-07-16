# ==============================================================================
# Shared config for the Opal-vs-Armadillo DataSHIELD benchmark.
# .env is the single source of truth (connections + run params); everything here
# just reads it. Sourced by every stage (directly or via helpers.R).
# ==============================================================================

# --- Load .env into the environment -----------------------------------------
# KEY=value lines, parsed in R (no shell eval); '# comments' stripped. Path from
# ENV_FILE (default ../.env, since stages run from lib/).
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

# --- Package library --------------------------------------------------------
# Use the benchmark's project-local library (BENCH_LIB, default .Rlib) built by
# install_benchmark_dependencies.R, isolated from the global R library. BENCH_LIB
# may be a comma-separated priority list; existing dirs are prepended.
libs <- Filter(dir.exists, trimws(strsplit(Sys.getenv("BENCH_LIB", ".Rlib"), ",")[[1]]))
if (length(libs)) {
  .libPaths(c(normalizePath(libs), .libPaths()))
} else {
  warning("BENCH_LIB not found - using the global R library; ",
          "run install_benchmark_dependencies.R first.", call. = FALSE)
}

suppressMessages({
  library(DSI)
  library(DSOpal)
  library(DSMolgenisArmadillo)
})

# --- Backends ---------------------------------------------------------------
# Backends are named <kind>_<location>[_rserve], e.g. armadillo_remote_rserve.
# BACKENDS lists which to run; each resolves its connection from location-prefixed
# .env keys (basic auth): {OPAL,ARMA}_{LOCAL,REMOTE}_{URL,USER,PASS}.
BACKENDS <- trimws(strsplit(Sys.getenv("BACKENDS",
  "opal_local,opal_remote,armadillo_local,armadillo_local_rserve,armadillo_remote,armadillo_remote_rserve"),
  ",")[[1]])

# Compute profile (R engine) per backend -- all must run the same pinned dsBase
# image for a valid comparison. Armadillo's two profiles are separate backends.
OPAL_PROFILE        <- Sys.getenv("OPAL_PROFILE",        "default")   # Opal rock
ARMA_PROFILE        <- Sys.getenv("ARMA_PROFILE",        "default")   # Armadillo rock
ARMA_RSERVE_PROFILE <- Sys.getenv("ARMA_RSERVE_PROFILE", "rserve")    # Armadillo rserve

require_env <- function(k) {
  v <- Sys.getenv(k)
  if (!nzchar(v)) stop(sprintf("%s is not set -- define it in your .env", k), call. = FALSE)
  v
}
backend_kind     <- function(be) if (grepl("^opal", be)) "opal" else "armadillo"
backend_location <- function(be) {
  if (grepl("_local", be)) {
    "local"
  } else if (grepl("_remote", be)) {
    "remote"
  } else {
    stop("backend '", be, "' must contain _local or _remote", call. = FALSE)
  }
}

# Resolve a backend name to {url, user, pass, driver, profile}. Env keys are
# required only for the backends actually used. Local servers run on localhost at
# {OPAL,ARMA}_LOCAL_PORT; remote backends give a full *_REMOTE_URL.
backend_spec <- function(be) {
  kind <- backend_kind(be)
  loc  <- backend_location(be)
  p    <- sprintf("%s_%s_", if (kind == "opal") "OPAL" else "ARMA", toupper(loc))
  url  <- if (loc == "local") {
    sprintf("http://localhost:%s", require_env(paste0(p, "PORT")))
  } else {
    require_env(paste0(p, "URL"))
  }
  list(be = be, kind = kind,
       driver  = if (kind == "opal") "OpalDriver" else "ArmadilloDriver",
       url     = url,
       user    = require_env(paste0(p, "USER")),
       pass    = require_env(paste0(p, "PASS")),
       profile = if (kind == "opal") OPAL_PROFILE
                 else if (grepl("_rserve$", be)) ARMA_RSERVE_PROFILE else ARMA_PROFILE)
}

# --- Data -------------------------------------------------------------------
# Tables are vendored (data/tables.rda, fixed 10k rows; see make_data.R). setup.R
# uploads them into PROJECT[/FOLDER]; the benchmark assigns each to a server symbol
# (D = CNSIM, D2 = CNSIM_B merge partner, DS = survival, DC = cluster).
PROJECT   <- Sys.getenv("PROJECT", "perf")
FOLDER    <- Sys.getenv("FOLDER", "bench")     # Armadillo only (Opal has no folders)
DATA_FILE <- Sys.getenv("DATA_FILE", file.path("..", "data", "tables.rda"))

DATASETS <- list(
  list(table = "CNSIM",    symbol = "D"),
  list(table = "CNSIM_B",  symbol = "D2"),
  list(table = "SURVIVAL", symbol = "DS"),
  list(table = "CLUSTER",  symbol = "DC")
)
TABLE_A <- "CNSIM"   # default login table

# Table name -> per-backend reference: Opal project.table; Armadillo project/folder/table.
ds_table_ref <- function(be, tbl)
  if (backend_kind(be) == "opal") paste0(PROJECT, ".", tbl) else paste(PROJECT, FOLDER, tbl, sep = "/")
table_a_ref  <- function(be) ds_table_ref(be, TABLE_A)

# --- Run parameters ---------------------------------------------------------
RESULTS_DIR <- Sys.getenv("RESULTS_DIR", "results")   # where CSVs + plots are written
# Poll interval (s) between "is it done?" checks -- for BOTH DSI's high-level
# polling (session ops) and the function suite's manual loop (helpers.R). Held
# tight so the wait stays negligible and doesn't distort fast local timings.
POLL_SLEEP <- as.numeric(Sys.getenv("POLL_SLEEP", "0.002"))
options(datashield.polling.sleep.0 = POLL_SLEEP)

# --- Login builder ----------------------------------------------------------
# Assemble a multi-server DSI logindata object over `backends`; subset per backend
# with login_for(). All backends use basic auth + a compute profile.
append_backend <- function(b, spec)
  b$append(server = spec$be, url = spec$url, user = spec$user, password = spec$pass,
           table = table_a_ref(spec$be), driver = spec$driver, profile = spec$profile)

build_logins <- function(backends = BACKENDS) {
  b <- DSI::newDSLoginBuilder(.silent = TRUE)
  for (be in backends) append_backend(b, backend_spec(be))
  b$build()
}
login_for <- function(logindata, be) logindata[logindata$server == be, , drop = FALSE]
