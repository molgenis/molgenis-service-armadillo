#!/usr/bin/env Rscript
# ==============================================================================
# One-off generator for the vendored benchmark tables (data/tables.rda).
#
#   Rscript make_data.R
#
# Builds the four benchmark tables from the dsBaseClient test fixtures, fixed at
# 10,000 rows x 30 columns, and saves them as a named list to data/tables.rda.
# setup.R just uploads that file -- the benchmark itself has NO size knobs. Rerun
# this only if you want to regenerate the vendored data (needs the fixtures).
#
# Point DSBASECLIENT_DATA at dsBaseClient's tests/testthat/data_files, or edit
# DATA_DIR below. Deterministic under the seed set here.
# ==============================================================================

# Resolve this script's own directory so output always lands in benchmark/data,
# regardless of the working directory it is run from.
.args    <- commandArgs(FALSE)
.file    <- sub("^--file=", "", .args[grep("^--file=", .args)])
BENCH    <- if (length(.file)) dirname(normalizePath(.file)) else getwd()

DATA_DIR <- Sys.getenv("DSBASECLIENT_DATA",
  file.path(Sys.getenv("HOME"), "git-repos/ds-core/dsBaseClient/tests/testthat/data_files"))
N_ROWS <- 10000L
N_VARS <- 30L
set.seed(42)

# Dataset registry: which fixture builds each table, and how to inflate it.
#   rda     - path under DATA_DIR of the dsBaseClient .rda
#   table   - output table name (the key in tables.rda)
#   kind    - "flat" resamples rows; "survival"/"cluster" tile + renumber id_cols
#   id_cols - identifier columns renumbered per tile (structured kinds)
#   slim    - keep only a few columns (the merge partner table)
DATASETS <- list(
  list(rda = "CNSIM/CNSIM1.rda", table = "CNSIM",   kind = "flat"),
  list(rda = "CNSIM/CNSIM1.rda", table = "CNSIM_B", kind = "flat", slim = TRUE),
  list(rda = "SURVIVAL/EXPAND_NO_MISSING/EXPAND_NO_MISSING1.rda",
       table = "SURVIVAL", kind = "survival", id_cols = c("id")),
  list(rda = "CLUSTER/CLUSTER_SLO1.rda", table = "CLUSTER",
       kind = "cluster", id_cols = c("idSurgery", "idDoctor"))
)

# Load the object stored in a .rda (the object name varies), returning the frame.
load_rda <- function(rel) {
  e  <- new.env()
  nm <- load(file.path(DATA_DIR, rel), envir = e)
  e[[nm]]
}

# Append synthetic columns (numeric / integer / factor in rotation) to n_vars.
pad_to <- function(df, n_vars) {
  need <- n_vars - ncol(df)
  if (need <= 0) return(df)
  n <- nrow(df)
  extra <- lapply(seq_len(need), function(i)
    switch((i - 1) %% 3 + 1,
           rnorm(n),
           sample.int(100, n, replace = TRUE),
           factor(sample(c("a", "b", "c"), n, replace = TRUE))))
  names(extra) <- paste0("x", seq_len(need))
  cbind(df, as.data.frame(extra, stringsAsFactors = FALSE))
}

# Flat datasets: upsample rows with replacement to n; add a unique Opal entity id
# and a `key` join column for ds.merge, then pad to N_VARS.
inflate_flat <- function(df, n, n_vars) {
  out <- df[sample.int(nrow(df), n, replace = TRUE), , drop = FALSE]
  rownames(out) <- NULL
  out <- cbind(entity_id = seq_len(n), key = seq_len(n), out)
  pad_to(out, n_vars)
}

# Structured datasets: tile the frame to >= n rows and offset id_cols per tile so
# identifiers stay unique (survival) / groups scale but stay valid (cluster).
inflate_struct <- function(df, n, id_cols, n_vars) {
  reps  <- ceiling(n / nrow(df))
  parts <- lapply(seq_len(reps), function(k) {
    p <- df
    for (cl in id_cols) {
      if (!cl %in% names(p)) next
      v <- p[[cl]]
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

tables <- list()
for (d in DATASETS) {
  raw <- load_rda(d$rda)
  tab <- if (d$kind == "flat") inflate_flat(raw, N_ROWS, N_VARS)
         else                  inflate_struct(raw, N_ROWS, d$id_cols, N_VARS)
  if (isTRUE(d$slim))
    tab <- tab[, c("entity_id", "key", "LAB_TRIG", "GENDER", "LAB_HDL")]
  tables[[d$table]] <- tab
  cat(sprintf("  %-10s %d x %d\n", d$table, nrow(tab), ncol(tab)))
}

out <- file.path(BENCH, "data", "tables.rda")
dir.create(dirname(out), showWarnings = FALSE)
save(tables, file = out, compress = "xz")
cat(sprintf("Wrote %s (%.0f KB)\n", out, file.size(out) / 1024))
