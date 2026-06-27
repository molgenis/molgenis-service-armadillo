# ==============================================================================
# Shared helpers, sourced by every benchmark script:
#   ops.R          -- the broad ds_ops registry (data, not behaviour)
#   bench.R        -- broad throughput survey       -> results/rates.csv
#   capture.R      -- extract serverside primitives  -> results/primitives.csv
#   probe.R        -- validate every call form (no timing)
#   speed_true.R   -- true server compute time (endDate - startDate)
#   speed_client.R -- client-observed time (high-level call, default poll-sleep)
#
# The two speed scripts run the SAME single-command serverside calls extracted
# into results/primitives.csv by capture.R, so the measurements are comparable.
# This file owns everything cross-cutting; each script stays top-to-bottom
# readable and does exactly one thing.
# ==============================================================================

source("config.R")          # loads DSI/DSOpal/DSMolgenisArmadillo + URLs/helpers
options(digits.secs = 6)

secs_since <- function(t0) as.numeric(Sys.time() - t0, units = "secs")

# Run `op` (a no-arg thunk) repeatedly until `secs` elapse; return count/elapsed/rate.
time_op <- function(op, secs) {
  n <- 0L; t0 <- Sys.time()
  repeat { op(); n <- n + 1L; if (secs_since(t0) >= secs) break }
  el <- secs_since(t0)
  data.frame(count = n, elapsed = el, rate = n / el)
}

# --- Connections ------------------------------------------------------------
# Build connections INSIDE a function so no bare connection object leaks into the
# global env (multiple visible connections confuse functions that auto-detect
# one, e.g. ds.skewness).
connect_be <- function(be, logindata = build_logins()) {
  cn <- datashield.login(login_for(logindata, be), assign = FALSE)
  for (d in DATASETS) datashield.assign.table(cn, d$symbol, ds_table_ref(be, d$table))
  cn
}

# Connect to each requested backend, assigning the standard datasets. Backends
# that are unavailable are skipped with a message (the run continues on the rest).
build_conns <- function(backends = BACKENDS, logindata = build_logins()) {
  cs <- list()
  for (be in backends) {
    cat(sprintf("Connecting + assigning tables on %s...\n", be))
    cn <- tryCatch(connect_be(be, logindata), error = function(e) {
      message(sprintf("  skipping backend '%s' (unavailable): %s", be, conditionMessage(e))); NULL })
    if (!is.null(cn)) cs[[be]] <- cn
  }
  cs
}

# Log out of every connection in a list, ignoring errors. Use with
# on.exit(logout_all(conns), add = TRUE) so cleanup runs even on error.
logout_all <- function(conns) {
  for (be in names(conns)) try(datashield.logout(conns[[be]]), silent = TRUE)
  if (exists(".dscn", envir = globalenv(), inherits = FALSE))
    try(rm(".dscn", envir = globalenv()), silent = TRUE)
}

# Designate a connection as the DSI default so functions that don't forward
# `datasources` to their internal calls still resolve it.
use_conn <- function(cn) {
  assign(".dscn", cn, envir = globalenv())
  DSI::datashield.connections_default(".dscn", env = globalenv())
}

# Keep only the base dataset symbols between tests; remove everything else so
# each function starts from the same clean state (as the dsBaseClient tests do).
BASE_SYMBOLS <- vapply(DATASETS, function(d) d$symbol, character(1))
reset <- function(cn) {
  info <- tryCatch(ds.ls(datasources = cn), error = function(e) NULL)
  for (o in setdiff(unique(unlist(lapply(info, `[[`, "objects.found"))), BASE_SYMBOLS))
    try(ds.rm(x.names = o, datasources = cn), silent = TRUE)
}

# --- Server-command timing (the one place that uses package-private APIs) ----
# Parse an ISO-8601 instant ("...Thh:mm:ss.SSSZ", or a +HH:MM offset). We only
# ever subtract two stamps from the SAME server, so stripping the zone and
# parsing both as naive-UTC leaves the difference exact regardless of its zone.
.parse_iso <- function(x) {
  if (is.null(x) || length(x) != 1 || is.na(x) || !nzchar(x)) return(as.POSIXct(NA))
  as.POSIXct(sub("([Zz]|[+-][0-9]{2}:?[0-9]{2})$", "", x),
             format = "%Y-%m-%dT%H:%M:%OS", tz = "UTC")
}

# True compute time (ms) of the just-completed command on a single node `conn`.
# Read BEFORE dsFetch. Armadillo: GET /lastcommand DTO; Opal: command-by-id.
# NOTE: uses package-private functions (DSMolgenisArmadillo:::.get_auth_header,
# DSOpal:::.datashield.command) -- there is no public API for the command record.
# Isolated here, tryCatch'd to NA, and a version-pin risk on a driver upgrade.
command_compute_ms <- function(conn, res) tryCatch({
  if (methods::is(conn, "ArmadilloConnection")) {
    r <- httr::GET(handle = conn@handle, path = "/lastcommand",
                   config = httr::add_headers(DSMolgenisArmadillo:::.get_auth_header(conn)))
    cmd <- httr::content(r)
  } else if (methods::is(conn, "OpalConnection")) {
    cmd <- DSOpal:::.datashield.command(conn@opal, res@rval$rid)
  } else stop("unsupported backend connection: ", paste(class(conn), collapse = "/"))
  d <- as.numeric(.parse_iso(cmd$endDate) - .parse_iso(cmd$startDate), units = "secs") * 1000
  if (length(d) == 1 && !is.na(d)) d else NA_real_
}, error = function(e) NA_real_)

# --- CSV I/O ----------------------------------------------------------------
# Incremental writer: header now, rows appended as we go (a crash never loses
# completed rows). Returns an append(df) function bound to `path`/`cols`.
open_csv <- function(path, cols) {
  dir.create(dirname(path), showWarnings = FALSE, recursive = TRUE)
  write.csv(setNames(data.frame(lapply(cols, function(x) character(0))), cols), path, row.names = FALSE)
  function(df) write.table(df[, cols], path, sep = ",", row.names = FALSE, col.names = FALSE, append = TRUE)
}

# --- Primitive set (the shared contract between capture and the speed scripts)
PRIM_CSV <- file.path(dirname(OUT_CSV), "primitives.csv")

# Extracted single-command serverside primitives (kind, fn, expr).
read_primitives <- function(path = PRIM_CSV) {
  if (!file.exists(path))
    stop("primitives.csv not found - run `Rscript capture.R` first.", call. = FALSE)
  read.csv(path, stringsAsFactors = FALSE)
}

# Submit one primitive ASYNC on a single node, returning the result handle. Only
# the two single-command serverside kinds the capture trace produces: an
# aggregate, or an arithmetic assign.expr. (assign.table is an I/O op with a
# backend-specific table path -- it is measured by the broad survey, not here.)
submit_primitive <- function(conn, kind, expr, symbol = "p_tmp") {
  if (kind == "aggregate") dsAggregate(conn, expr, async = TRUE)
  else                     dsAssignExpr(conn, symbol, expr, async = TRUE)
}

# Run one primitive via the high-level DSI call (default poll-sleep) on `conns`.
run_primitive_hl <- function(conns, kind, expr, symbol = "p_tmp") {
  if (kind == "aggregate") datashield.aggregate(conns, expr)
  else                     datashield.assign.expr(conns, symbol, expr)
}

# Trace the serverside calls each op issues and return the DISTINCT (kind, expr)
# pairs. Tracing DSI's internals is the hacky part -- it is contained here and
# ALWAYS untraced on exit. The tracer runs in DSI's namespace, so the recorder is
# exposed under a temporary global name (removed on exit) rather than leaked.
.capture_serverside_calls <- function(conn, ops) {
  use_conn(conn)
  cap <- new.env(parent = emptyenv()); cap$list <- list()
  rec <- function(kind, e)
    cap$list[[length(cap$list) + 1L]] <-
      c(kind, if (is.character(e)) e else paste(deparse(e), collapse = " "))
  assign(".prim_rec", rec, envir = globalenv())
  on.exit({
    suppressMessages({
      try(untrace("datashield.aggregate",   where = asNamespace("DSI")), silent = TRUE)
      try(untrace("datashield.assign.expr", where = asNamespace("DSI")), silent = TRUE)
    })
    try(rm(".prim_rec", envir = globalenv()), silent = TRUE)
  }, add = TRUE)
  suppressMessages({
    trace("datashield.aggregate",   tracer = quote(.GlobalEnv$.prim_rec("aggregate", expr)), print = FALSE, where = asNamespace("DSI"))
    trace("datashield.assign.expr", tracer = quote(.GlobalEnv$.prim_rec("assign",    expr)), print = FALSE, where = asNamespace("DSI"))
  })
  calls <- list()
  for (o in ops) {
    reset(conn)
    cap$list <- list()
    suppressWarnings(suppressMessages(try(o$fn(conn), silent = TRUE)))
    for (cl in cap$list) {
      key <- paste0(cl[1], "\t", cl[2])
      if (is.null(calls[[key]])) calls[[key]] <- cl
    }
  }
  calls
}

# Keep only the calls that run standalone on a freshly reset connection: drop pure
# plumbing (existence / message checks) and calls that error in isolation (those
# reference mid-op intermediate objects). Returns a data.frame(kind, fn, expr).
.validate_standalone <- function(conn, calls) {
  is_plumbing <- function(expr) grepl("^(testObjExistsDS|messageDS|exists)\\(", expr)
  rows <- list()
  for (cl in calls) {
    kind <- cl[1]; expr <- cl[2]
    if (is_plumbing(expr)) next
    reset(conn)
    ok <- tryCatch({
      if (kind == "aggregate") datashield.aggregate(conn, expr)
      else datashield.assign.expr(conn, "p_tmp", expr)
      TRUE
    }, error = function(e) FALSE)
    fn <- sub("\\(.*$", "", expr)
    cat(sprintf("%-9s %-22s %s\n", kind, fn, if (ok) "OK" else "skip (not standalone)"))
    if (ok) rows[[length(rows) + 1L]] <- data.frame(kind = kind, fn = fn, expr = expr, stringsAsFactors = FALSE)
  }
  if (length(rows)) do.call(rbind, rows)
  else data.frame(kind = character(0), fn = character(0), expr = character(0))
}

# Extract single-command serverside primitives from the op registry into
# results/primitives.csv: capture the distinct calls, keep the standalone ones,
# write. `ops` is flatten_ops(be) from ops.R; `conn` is one backend's connection.
write_primitives <- function(conn, ops, path = PRIM_CSV) {
  out <- .validate_standalone(conn, .capture_serverside_calls(conn, ops))
  dir.create(dirname(path), showWarnings = FALSE, recursive = TRUE)
  write.csv(out, path, row.names = FALSE)
  out
}

# --- Primitive speed suite (shared by speed_true.R / speed_client.R) ---------
TIGHT_POLL_SEC <- 0.002         # client poll interval for the low-level round trip

# For each backend x primitive: warm up (untimed), time `reps` reps, append one
# row per rep to `out`, and print the per-primitive median. `measure(target, kind,
# expr)` returns a named numeric vector of millisecond metrics; its names must
# match `metrics` and become the CSV's metric columns. node = TRUE passes the
# single low-level node conns[[be]][[1]] (async submit + server-timestamp read);
# node = FALSE passes the high-level connection conns[[be]].
run_speed_suite <- function(prims, conns, reps, out, metrics, measure, node = FALSE) {
  cols <- c("backend", "pid", "fn", "kind", "rep", metrics)
  append_rows <- open_csv(out, cols)
  cat(sprintf("%d primitives x %d reps x %d backend(s) -> %s\n",
              nrow(prims), reps, length(conns), out))
  for (be in names(conns)) {
    target <- if (node) conns[[be]][[1]] else conns[[be]]
    cat(sprintf("\n== %s ==\n", be))
    for (i in seq_len(nrow(prims))) {
      kind <- prims$kind[i]; expr <- prims$expr[i]; fn <- prims$fn[i]
      try(measure(target, kind, expr), silent = TRUE)            # warm-up (excluded)
      m <- matrix(NA_real_, reps, length(metrics), dimnames = list(NULL, metrics))
      for (r in seq_len(reps)) try(m[r, ] <- measure(target, kind, expr), silent = TRUE)
      row <- data.frame(backend = be, pid = i, fn = fn, kind = kind, rep = seq_len(reps))
      for (mt in metrics) row[[mt]] <- round(m[, mt], 3)
      append_rows(row)
      meds <- apply(m, 2, median, na.rm = TRUE)
      cat(sprintf("  %-18s %s ms (median, n=%d)\n", fn,
                  paste(sprintf("%s %7.2f", metrics, meds), collapse = " | "),
                  sum(!is.na(m[, 1]))))
    }
  }
  logout_all(conns)
  cat(sprintf("\nWrote %s\n", out))
}
