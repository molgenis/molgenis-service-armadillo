# ==============================================================================
# One-time setup: build the benchmark tables from REAL dsBaseClient test data,
# upload them to BOTH backends, and save a DataSHIELD workspace on each (so the
# benchmark can time workspace loading).
#
#   Rscript setup.R
#
# Data comes from the dsBaseClient package's tests/testthat/data_files (see
# DATA_DIR / DATASETS in config.R). Each dataset is loaded from its .rda and
# inflated to N_ROWS rows x ~N_VARS columns, mirroring the datasets the
# dsBaseClient tests use for each function.
#
# Requires both servers running (see start_servers.sh). Idempotent: tables and
# workspaces that already exist are left alone (set FORCE=1 to overwrite tables).
# ==============================================================================

source("config.R")
suppressMessages({
  library(opalr)
  library(MolgenisArmadillo)
  library(tibble)
})

force <- nzchar(Sys.getenv("FORCE"))
set.seed(42)

# --- 1. Build tables from dsBaseClient data ---------------------------------
# Load the object stored in a dsBaseClient .rda (the object name varies, e.g.
# "study1"), returning the data frame.
load_rda <- function(rel) {
  e  <- new.env()
  nm <- load(file.path(DATA_DIR, rel), envir = e)
  e[[nm]]
}

# Append synthetic columns (numeric / integer / factor in rotation) until the
# frame has `n_vars` columns. Deterministic under the seed set above.
pad_to <- function(df, n_vars) {
  need <- n_vars - ncol(df)
  if (need <= 0) return(df)
  n <- nrow(df)
  extra <- lapply(seq_len(need), function(i) {
    switch((i - 1) %% 3 + 1,
           rnorm(n),
           sample.int(100, n, replace = TRUE),
           factor(sample(c("a", "b", "c"), n, replace = TRUE)))
  })
  names(extra) <- paste0("x", seq_len(need))
  cbind(df, as.data.frame(extra, stringsAsFactors = FALSE))
}

# Flat datasets (CNSIM, GAMLSS, ...): upsample rows with replacement to n,
# preserving distributions / factor levels / NAs. Prepend a unique Opal entity
# id (`entity_id`, hidden by Opal from the assigned frame) and a `key` join
# column for ds.merge, then pad to ~N_VARS.
inflate_flat <- function(df, n, n_vars) {
  out <- df[sample.int(nrow(df), n, replace = TRUE), , drop = FALSE]
  rownames(out) <- NULL
  out <- cbind(entity_id = seq_len(n), key = seq_len(n), out)
  pad_to(out, n_vars)
}

# Structured datasets (survival subjects, clustered patients/doctors): tile the
# frame to >= n rows and offset id_cols per tile so identifiers stay unique
# (survival) and groups scale up but stay valid (cluster). Add the unique Opal
# entity id, then pad. Native id columns (e.g. survival `id`) are kept visible.
inflate_struct <- function(df, n, id_cols, n_vars) {
  reps  <- ceiling(n / nrow(df))
  parts <- lapply(seq_len(reps), function(k) {
    p <- df
    for (cl in id_cols) {
      if (!cl %in% names(p)) next
      v <- p[[cl]]
      # numeric ids: offset per tile to stay unique; factor/character grouping
      # ids: suffix the tile index so groups multiply but remain distinct.
      p[[cl]] <- if (is.numeric(v)) v + (k - 1L) * (max(df[[cl]], na.rm = TRUE) + 1L)
                 else factor(paste0(as.character(v), "_t", k))
    }
    p
  })
  out <- do.call(rbind, parts)[seq_len(n), , drop = FALSE]
  rownames(out) <- NULL
  out <- cbind(entity_id = seq_len(n), out)
  pad_to(out, n_vars)
}

build_tables <- function(n) {
  tabs <- list()
  for (d in DATASETS) {
    raw <- load_rda(d$rda)
    tab <- if (d$kind == "flat") inflate_flat(raw, n, N_VARS)
           else                  inflate_struct(raw, n, d$id_cols, N_VARS)
    if (isTRUE(d$slim))
      tab <- tab[, c("entity_id", "key", "LAB_TRIG", "GENDER", "LAB_HDL")]
    tabs[[d$table]] <- tab
  }
  tabs
}

cat(sprintf("Building %d benchmark tables from dsBaseClient data (%d rows)...\n",
            length(DATASETS), N_ROWS))
tables <- build_tables(N_ROWS)

for (nm in names(tables))
  cat(sprintf("  %-12s %d x %d\n", nm, nrow(tables[[nm]]), ncol(tables[[nm]])))

# --- 2. Upload to Opal ------------------------------------------------------
upload_opal <- function(tables) {
  cat("\n== Opal ==\n")
  o <- opal.login(OPAL_USER, OPAL_PASS, url = OPAL_URL)
  on.exit(opal.logout(o), add = TRUE)

  if (!opal.project_exists(o, PROJECT)) {
    dbs <- opal.projects_databases(o)
    db  <- if (is.data.frame(dbs)) dbs$name[1] else dbs[[1]]
    if (is.null(db) || is.na(db)) stop("No Opal data database available to host project '", PROJECT, "'.")
    cat(sprintf("Creating project '%s' on database '%s'\n", PROJECT, db))
    opal.project_create(o, PROJECT, database = db)
  }

  for (tbl in names(tables)) {
    if (!force && opal.table_exists(o, PROJECT, tbl)) {
      cat(sprintf("skip (exists): %s.%s\n", PROJECT, tbl)); next
    }
    opal.table_save(o, as_tibble(tables[[tbl]]), PROJECT, tbl,
                    overwrite = TRUE, force = TRUE, id.name = "entity_id")
    cat(sprintf("uploaded: %s.%s\n", PROJECT, tbl))
  }
}

# --- 3. Upload to Armadillo -------------------------------------------------
upload_arma <- function(tables) {
  cat("\n== Armadillo ==\n")
  armadillo.login_basic(ARMA_URL, ARMA_USER, ARMA_PASS)
  if (!(PROJECT %in% armadillo.list_projects())) {
    cat(sprintf("Creating project '%s'\n", PROJECT))
    armadillo.create_project(PROJECT)
  }
  existing <- tryCatch(armadillo.list_tables(PROJECT), error = function(e) character(0))
  for (tbl in names(tables)) {
    exists_tbl <- any(grepl(paste0(FOLDER, "/", tbl, "$"), existing))
    if (exists_tbl && !force) {
      cat(sprintf("skip (exists): %s/%s/%s\n", PROJECT, FOLDER, tbl)); next
    }
    if (exists_tbl) armadillo.delete_table(PROJECT, FOLDER, tbl)   # force: overwrite
    armadillo.upload_table(PROJECT, FOLDER, tables[[tbl]], tbl)
    cat(sprintf("uploaded: %s/%s/%s\n", PROJECT, FOLDER, tbl))
  }
}

# DRY_RUN=1 builds + reports the tables locally without touching the servers
# (used to validate inflation shapes; e.g. N_ROWS=1000 DRY_RUN=1 Rscript setup.R).
if (nzchar(Sys.getenv("DRY_RUN"))) {
  cat("\nDRY_RUN: tables built locally; skipping upload + workspace save.\n")
  quit(save = "no")
}

upload_opal(tables)
upload_arma(tables)

# --- 4. Save a DataSHIELD workspace per backend -----------------------------
# Logs in (assigning the CNSIM table to D), saves the session as WORKSPACE, logs
# out. datashield.login(restore = WORKSPACE) in the benchmark then has data to
# load.
save_workspace <- function(be) {
  cat(sprintf("\n== Workspace (%s) ==\n", be))
  ld <- login_for(build_logins(), be)
  cn <- datashield.login(ld, assign = FALSE)
  datashield.assign.table(cn, "D", table_a_ref(be))
  datashield.workspace_save(cn, WORKSPACE)
  datashield.logout(cn)
  cat(sprintf("saved workspace '%s' on %s\n", WORKSPACE, be))
}
for (be in BACKENDS)
  tryCatch(save_workspace(be), error = function(e)
    message(sprintf("workspace save skipped on %s (unavailable): %s", be, conditionMessage(e))))

cat("\nSetup complete.\n")
