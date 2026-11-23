# ==============================================================================
# One-time setup: upload the vendored benchmark tables to BOTH backends.
#
#   Rscript setup.R
#
# The tables are pre-built and fixed at 10,000 rows (data/tables.rda, made once
# by make_data.R from dsBaseClient test data). Each table is (re)uploaded fresh,
# overwriting any existing copy, so a run always reflects the vendored data.
#
# Requires both servers running. The timed workspace_load in measure.R saves +
# restores its own workspace, so none is created here.
# ==============================================================================

source("config.R")
suppressMessages({
  library(opalr)
  library(MolgenisArmadillo)
  library(tibble)
})

if (!file.exists(DATA_FILE))
  stop("vendored data not found: ", DATA_FILE, " -- run make_data.R first", call. = FALSE)
load(DATA_FILE)            # -> `tables`, a named list of data frames
cat(sprintf("Loaded %d tables from %s\n", length(tables), DATA_FILE))
for (nm in names(tables))
  cat(sprintf("  %-10s %d x %d\n", nm, nrow(tables[[nm]]), ncol(tables[[nm]])))

# --- Upload to Opal ---------------------------------------------------------
upload_opal <- function(tables, spec) {
  cat(sprintf("\n== Opal: %s (%s) ==\n", spec$be, spec$url))
  o <- opal.login(spec$user, spec$pass, url = spec$url)
  on.exit(opal.logout(o), add = TRUE)

  if (!opal.project_exists(o, PROJECT)) {
    dbs <- opal.projects_databases(o)
    db  <- if (is.data.frame(dbs)) dbs$name[1] else dbs[[1]]
    if (is.null(db) || is.na(db)) stop("No Opal data database available to host project '", PROJECT, "'.")
    cat(sprintf("Creating project '%s' on database '%s'\n", PROJECT, db))
    opal.project_create(o, PROJECT, database = db)
  }

  for (tbl in names(tables)) {
    opal.table_save(o, as_tibble(tables[[tbl]]), PROJECT, tbl,
                    overwrite = TRUE, force = TRUE, id.name = "entity_id")
    cat(sprintf("uploaded: %s.%s\n", PROJECT, tbl))
  }
}

# --- Upload to Armadillo ----------------------------------------------------
upload_arma <- function(tables, spec) {
  cat(sprintf("\n== Armadillo: %s (%s) ==\n", spec$be, spec$url))
  armadillo.login_basic(spec$url, spec$user, spec$pass)
  if (!(PROJECT %in% armadillo.list_projects())) {
    cat(sprintf("Creating project '%s'\n", PROJECT))
    armadillo.create_project(PROJECT)
  }
  existing <- tryCatch(armadillo.list_tables(PROJECT), error = function(e) character(0))
  for (tbl in names(tables)) {
    if (any(grepl(paste0(FOLDER, "/", tbl, "$"), existing)))
      armadillo.delete_table(PROJECT, FOLDER, tbl)     # overwrite
    armadillo.upload_table(PROJECT, FOLDER, tables[[tbl]], tbl)
    cat(sprintf("uploaded: %s/%s/%s\n", PROJECT, FOLDER, tbl))
  }
}

# Upload once per distinct host (kind + location). Armadillo's default and rserve
# profiles share one server/storage, so they collapse to a single upload.
hosts <- unique(vapply(BACKENDS,
  function(be) paste0(backend_kind(be), "_", backend_location(be)), character(1)))
for (h in hosts) {
  spec   <- backend_spec(h)
  upload <- if (spec$kind == "opal") upload_opal else upload_arma
  tryCatch(upload(tables, spec),
    error = function(e) message(sprintf("upload to %s failed: %s", h, conditionMessage(e))))
}

# --- Local Armadillo 'rserve' profile ---------------------------------------
# The armadillo_local_rserve backend needs an 'rserve' profile on the local
# Armadillo (the 'default' profile ships with it). Create + start it on
# RSERVE_IMAGE (dsBase whitelisted, permissive). Remote profiles must pre-exist.
ensure_local_rserve <- function() {
  if (!"armadillo_local_rserve" %in% BACKENDS) return(invisible())
  spec <- backend_spec("armadillo_local")
  img  <- Sys.getenv("RSERVE_IMAGE", "datashield/rserver_soccer-donkey:latest")
  port <- as.integer(Sys.getenv("RSERVE_PORT", "6315"))   # default is 6311; rserve on its own port
  auth <- httr::authenticate(spec$user, spec$pass)
  cat(sprintf("\n== Armadillo rserve profile '%s' on %s (:%d) ==\n", ARMA_RSERVE_PROFILE, img, port))
  r <- httr::PUT(paste0(spec$url, "/ds-profiles"), auth, encode = "json",
                 body = list(name = ARMA_RSERVE_PROFILE, image = img, host = "localhost", port = port,
                             packageWhitelist = list("dsBase"), functionBlacklist = list(),
                             options = list(datashield.privacyControlLevel = "permissive")))
  if (!httr::status_code(r) %in% c(200L, 204L))
    message(sprintf("  profile create returned %d: %s", httr::status_code(r),
                    httr::content(r, "text", encoding = "UTF-8")))
  httr::POST(paste0(spec$url, "/ds-profiles/", ARMA_RSERVE_PROFILE, "/start"), auth)
}
tryCatch(ensure_local_rserve(), error = function(e)
  message(sprintf("rserve profile setup skipped: %s", conditionMessage(e))))

cat("\nSetup complete.\n")
