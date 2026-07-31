# ==============================================================================
# Shared helpers + data, sourced by measure.R (and plots.R for PRIMITIVES):
#   - PRIMITIVES / FAM_ORDER (the serverside calls to time + their families)
#   - connections (connect_be)
#   - true server-command timing (command_compute_ms, the one private-API use)
#   - incremental CSV writer (open_csv)
#   - the fair, interleaved speed-suite runner (run_speed_suite)
#
# Loads config.R (URLs/creds/params + the DS packages). Each stage script stays
# top-to-bottom readable; everything cross-cutting lives here.
# ==============================================================================

source("config.R")          # loads DSI/DSOpal/DSMolgenisArmadillo + URLs/helpers
options(digits.secs = 6)

secs_since <- function(t0) as.numeric(Sys.time() - t0, units = "secs")

# --- Connections ------------------------------------------------------------
# Build connections INSIDE a function so no bare connection object leaks into the
# global env (multiple visible connections confuse functions that auto-detect
# one, e.g. ds.skewness).
connect_be <- function(be, logindata = build_logins()) {
  cn <- datashield.login(login_for(logindata, be), assign = FALSE)
  for (d in DATASETS) datashield.assign.table(cn, d$symbol, ds_table_ref(be, d$table))
  cn
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

# --- Primitive submission ---------------------------------------------------
# Submit one primitive ASYNC on a single node, returning the result handle. The
# two single-command serverside kinds: an aggregate, or an arithmetic/serverside
# assign.expr (assign.table is an I/O op measured as a session op in measure.R).
submit_primitive <- function(conn, kind, expr, symbol = "p_tmp") {
  if (kind == "aggregate") dsAggregate(conn, expr, async = TRUE)
  else                     dsAssignExpr(conn, symbol, expr, async = TRUE)
}

# --- Primitive speed suite --------------------------------------------------
TIGHT_POLL_SEC <- POLL_SLEEP   # manual poll interval (s), from .env (POLL_SLEEP)

# Fair, interleaved timing. ALL backends are connected up front and kept live, and
# every primitive is timed on every backend back-to-back within each pass -- so
# Opal and Armadillo for the same op are measured in the same moment (same network
# conditions), making the comparison fair rather than block-vs-block. The `reps`
# total reps per cell are split across SPEED_SETS passes (default 5); the primitive
# order and backend order are reshuffled each pass (seeded via SEED) to cancel
# warm-up and slow drift. `measure(target, kind, expr)` returns a named numeric
# vector of ms metrics matching `metrics`. node = TRUE passes the single low-level
# node conns[[be]][[1]]; node = FALSE the high-level connection conns[[be]].
run_speed_suite <- function(prims, backends, reps, out, metrics, measure, node = FALSE) {
  sets         <- max(1L, as.integer(Sys.getenv("SPEED_SETS", "5")))
  reps_per_set <- max(1L, reps)          # `reps` (SPEED_REPS) is reps PER pass
  total        <- sets * reps_per_set    # total per cell = sets x reps
  set.seed(as.integer(Sys.getenv("SEED", "1")))
  logindata <- build_logins(backends)
  cols <- c("backend", "pid", "fn", "kind", "set", "rep", metrics)
  append_rows <- open_csv(out, cols)

  connect1 <- function(be) tryCatch(connect_be(be, logindata), error = function(e) {
    message(sprintf("  connect failed for %s: %s", be, conditionMessage(e))); NULL })
  conns <- list()
  for (be in backends) { cn <- connect1(be); if (!is.null(cn)) conns[[be]] <- cn }
  active <- names(conns)
  # Guarantee logout on ANY exit (normal or error), so no server session lingers.
  on.exit(for (be in names(conns)) try(datashield.logout(conns[[be]]), silent = TRUE), add = TRUE)
  tgt <- function(be) if (node) conns[[be]][[1]] else conns[[be]]

  cat(sprintf("%d primitives x %d sets x %d reps x %d backend(s) = n=%d -> %s\n",
              nrow(prims), sets, reps_per_set, length(active), total, out))

  np  <- nrow(prims)
  ky  <- function(be, i) paste(be, i, sep = "\t")
  acc <- new.env(parent = emptyenv())   # per-cell accumulated metric matrix

  for (s in seq_len(sets)) {
    cat(sprintf("\n-- set %d/%d --\n", s, sets))
    for (i in sample(np)) {
      kind <- prims$kind[i]; expr <- prims$expr[i]; fn <- prims$fn[i]
      for (be in sample(active)) {
        if (s == 1L) try(measure(tgt(be), kind, expr), silent = TRUE)  # one warm-up per cell
        m <- matrix(NA_real_, reps_per_set, length(metrics), dimnames = list(NULL, metrics))
        for (r in seq_len(reps_per_set))
          m[r, ] <- tryCatch(measure(tgt(be), kind, expr),
            error = function(e) { conns[[be]] <<- connect1(be); rep(NA_real_, length(metrics)) })
        row <- data.frame(backend = be, pid = i, fn = fn, kind = kind, set = s,
                          rep = (s - 1L) * reps_per_set + seq_len(reps_per_set))
        for (mt in metrics) row[[mt]] <- round(m[, mt], 3)
        append_rows(row)
        k <- ky(be, i); acc[[k]] <- rbind(acc[[k]], m)
      }
    }
  }

  cat(sprintf("\n== medians (n = %d) ==\n", total))
  for (be in active) for (i in seq_len(np)) {
    mm <- acc[[ky(be, i)]]; if (is.null(mm)) next
    meds <- apply(mm, 2, median, na.rm = TRUE)
    cat(sprintf("  %-22s %-18s %s ms (median, n=%d)\n", be, prims$fn[i],
                paste(sprintf("%s %7.2f", metrics, meds), collapse = " | "),
                sum(!is.na(mm[, 1]))))
  }
  cat(sprintf("\nWrote %s\n", out))   # conns logged out by on.exit above
}

# --- The serverside primitives to time --------------------------------------
# Exactly one aggregate/assign expression per benchmarked dsBase function, grouped
# into the six families the methodology reports. Each is the call the dsBaseClient
# function ultimately issues to the server -- previously extracted at runtime by
# tracing DSI internals; pinned here for dsBase 6.3.5 so no capture step is needed.
# Symbols D = CNSIM, D2 = CNSIM_B, DS = survival, DC = cluster are assigned by
# connect_be() (see config.R DATASETS). The session ops (login, workspace, ...)
# are timed separately in measure.R. Used by measure.R (to time) and plots.R
# (for the fn -> family map).
prim <- function(family, fn, kind, expr)
  data.frame(family = family, fn = fn, kind = kind, expr = expr, stringsAsFactors = FALSE)

PRIMITIVES <- rbind(
  # --- Summary statistics ---------------------------------------------------
  prim("Summary statistics", "meanDS",         "aggregate", "meanDS(D$LAB_TSC)"),
  prim("Summary statistics", "varDS",          "aggregate", "varDS(D$LAB_TSC)"),
  prim("Summary statistics", "quantileMeanDS", "aggregate", "quantileMeanDS(D$LAB_HDL)"),
  prim("Summary statistics", "corDS",          "aggregate", 'corDS("D$LAB_TSC", "D$LAB_HDL")'),
  prim("Summary statistics", "tableDS",        "aggregate", 'tableDS(rvar.transmit = "D$GENDER", cvar.transmit = "D$DIS_CVA", stvar.transmit = NULL, rvar.all.unique.levels.transmit = "0,1", cvar.all.unique.levels.transmit = "0", stvar.all.unique.levels.transmit = NULL, exclude.transmit = NULL, useNA.transmit = "always", force.nfilter.transmit = NULL)'),

  # --- Metadata / introspection ---------------------------------------------
  prim("Metadata", "classDS",     "aggregate", 'classDS("D$LAB_TSC")'),
  prim("Metadata", "dimDS",       "aggregate", 'dimDS("D")'),
  prim("Metadata", "colnamesDS",  "aggregate", 'colnamesDS("D")'),
  prim("Metadata", "lengthDS",    "aggregate", 'lengthDS("D$LAB_TSC")'),
  prim("Metadata", "levelsDS",    "aggregate", "levelsDS(D$GENDER)"),
  prim("Metadata", "numNaDS",     "aggregate", "numNaDS(D$LAB_HDL)"),
  prim("Metadata", "lsDS",        "aggregate", "lsDS(search.filter = NULL, 1L)"),
  prim("Metadata", "isValidDS",   "aggregate", "isValidDS(D$LAB_TSC)"),

  # --- Transform & recode ---------------------------------------------------
  prim("Transform & recode", "asFactorDS1",      "aggregate", 'asFactorDS1("D$DIS_CVA")'),
  prim("Transform & recode", "asFactorDS2",      "assign",    'asFactorDS2("D$DIS_CVA", "0", FALSE, 1)'),
  prim("Transform & recode", "asIntegerDS",      "assign",    'asIntegerDS("D$GENDER")'),
  prim("Transform & recode", "asCharacterDS",    "assign",    'asCharacterDS("D$GENDER")'),
  prim("Transform & recode", "asDataMatrixDS",   "assign",    'asDataMatrixDS("D$GENDER")'),
  prim("Transform & recode", "BooleDS",          "assign",    'BooleDS("D$LAB_TSC", "D$LAB_TRIG", 1, "NA", TRUE)'),
  prim("Transform & recode", "recodeValuesDS",   "assign",    'recodeValuesDS("D$DIS_CVA", "0,1", "10,20", NULL)'),
  prim("Transform & recode", "recodeLevelsDS",   "assign",    "recodeLevelsDS(D$GENDER, vectorDS('g0','g1'))"),
  prim("Transform & recode", "changeRefGroupDS", "assign",    "changeRefGroupDS(D$GENDER,'1',FALSE)"),
  prim("Transform & recode", "repDS",            "assign",    'repDS(x1.transmit = "4", times.transmit = "6", length.out.transmit = "NA", each.transmit = "1", x1.includes.characters = FALSE, source.x1 = "clientside", source.times = "clientside", source.length.out = NULL, source.each = "clientside")'),
  prim("Transform & recode", "replaceNaDS",      "assign",    "replaceNaDS(D$LAB_HDL, vectorDS(0))"),

  # --- Data-frame manipulation ----------------------------------------------
  prim("Data-frame", "dataFrameDS",        "assign",    'dataFrameDS("D$LAB_TSC,D$LAB_HDL", NULL, FALSE, TRUE, "D$LAB_TSC,D$LAB_HDL", TRUE, FALSE)'),
  prim("Data-frame", "dataFrameSubsetDS1", "aggregate", 'dataFrameSubsetDS1("D", "D$LAB_TSC", "D$LAB_HDL", 2, NULL, NULL, FALSE)'),
  prim("Data-frame", "dataFrameSubsetDS2", "assign",    'dataFrameSubsetDS2("D", "D$LAB_TSC", "D$LAB_HDL", 2, NULL, NULL, FALSE)'),
  prim("Data-frame", "cbindDS",            "assign",    'cbindDS("D$LAB_TSC,D$LAB_HDL", "D$LAB_TSC,D$LAB_HDL")'),
  prim("Data-frame", "mergeDS",            "assign",    'mergeDS("D", "D2", "key", "key", FALSE, FALSE, TRUE, ".x,.y", TRUE, NULL)'),
  prim("Data-frame", "reShapeDS",          "assign",    'reShapeDS("DS", NULL, "age.60", "time.id", "id", NULL, "wide", ".")'),

  # --- Modelling ------------------------------------------------------------
  prim("Modelling", "glmDS1",           "aggregate", 'glmDS1(LAB_TSC ~ LAB_TRIG, "gaussian", NULL, NULL, "D")'),
  prim("Modelling", "glmDS2",           "aggregate", 'glmDS2(LAB_TSC ~ LAB_TRIG, "gaussian", "0,0", NULL, NULL, "D")'),
  prim("Modelling", "glmSLMADS1",       "aggregate", 'glmSLMADS1(LAB_TSC ~ LAB_TRIG, "gaussian", NULL, NULL, "D")'),
  prim("Modelling", "glmSLMADS.assign", "assign",    'glmSLMADS.assign(LAB_TSC ~ LAB_TRIG, "gaussian", NULL, NULL, "D")'),
  prim("Modelling", "lmerSLMADS2",      "aggregate", 'lmerSLMADS2(incid_rate ~ trtGrp + Male + yyy1xxxidDoctorzzz, NULL, NULL, "DC", TRUE, NULL, NULL, NULL, 0)'),

  # --- DSI (rmDS; the session ops are timed separately in measure.R) ---------
  prim("DSI", "rmDS", "aggregate", 'rmDS("torm")')
)

# Family display order (shared by the plots).
FAM_ORDER <- c("Summary statistics", "Metadata", "Transform & recode",
               "Data-frame", "Modelling", "DSI")
