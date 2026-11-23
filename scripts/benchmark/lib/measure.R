# ==============================================================================
# The measurement stage. Two timed suites over all BACKENDS, fair-interleaved:
#
#   1. FUNCTIONS  -- the hard-coded serverside primitives (PRIMITIVES, helpers.R). For
#      each, the server's own execution time (endDate - startDate) plus a
#      tight-poll round trip. -> results/speed_ds_base.csv
#      (backend, pid, fn, kind, set, rep, compute_ms, roundtrip_ms)
#
#   2. SESSIONS   -- DSI orchestration ops (login, workspace_save/load,
#      assign.table, tables, ...). These are not single dsBase commands, so only
#      the client round trip is timed. -> results/speed_dsi.csv
#      (backend, op, set, rep, roundtrip_ms)
#
#   Rscript measure.R
#   CHECK=1 Rscript measure.R                   # validate every call, no timing
#
# CHECK runs each primitive + session op ONCE per backend and reports OK/FAIL --
# a fast pre-flight before a long timed run (replaces the old probe stage).
# ==============================================================================

source("helpers.R")            # config.R + shared helpers + PRIMITIVES

REPS     <- as.integer(Sys.getenv("SPEED_REPS", "4"))   # reps per pass (x SPEED_SETS passes)
SETS     <- max(1L, as.integer(Sys.getenv("SPEED_SETS", "5")))
RPS      <- max(1L, REPS)
FUN_CSV  <- Sys.getenv("SPEED_FUNCTIONS_CSV", file.path(RESULTS_DIR, "speed_ds_base.csv"))
SESS_CSV <- Sys.getenv("SPEED_SESSIONS_CSV",  file.path(RESULTS_DIR, "speed_dsi.csv"))
WS       <- "benchws"

timed <- function(f) { s <- Sys.time(); f(); secs_since(s) * 1000 }

# Session ops timed on a live connection (round trip only). login/workspace_load
# are handled separately below (each needs a FRESH login on the clock).
SESSION_INLOOP <- list(
  tables         = function(cn, be) datashield.tables(cn),
  profiles       = function(cn, be) datashield.profiles(cn),
  workspaces     = function(cn, be) datashield.workspaces(cn),
  pkg_status     = function(cn, be) datashield.pkg_status(cn),
  assign.table   = function(cn, be) datashield.assign.table(cn, "dsi_tmp", table_a_ref(be)),
  workspace_save = function(cn, be) datashield.workspace_save(cn, WS)
)

# =============================================================================
# CHECK mode: run each call once per backend, report OK/FAIL, exit.
# =============================================================================
if (nzchar(Sys.getenv("CHECK"))) {
  logindata <- build_logins(BACKENDS)
  for (be in BACKENDS) {
    cn <- tryCatch(connect_be(be, logindata), error = function(e) {
      message(sprintf("  skip %s (unavailable): %s", be, conditionMessage(e))); NULL })
    if (is.null(cn)) next
    cat(sprintf("\n== CHECK %s (%d primitives + %d session ops) ==\n",
                be, nrow(PRIMITIVES), length(SESSION_INLOOP) + 2L))
    nok <- 0L
    for (i in seq_len(nrow(PRIMITIVES))) {
      kind <- PRIMITIVES$kind[i]; expr <- PRIMITIVES$expr[i]
      msg <- tryCatch({
        if (kind == "aggregate") datashield.aggregate(cn, expr)
        else datashield.assign.expr(cn, "p_tmp", expr)
        nok <- nok + 1L; "OK"
      }, error = function(e) paste("FAIL:", conditionMessage(e)))
      cat(sprintf("  %-9s %-20s %s\n", kind, PRIMITIVES$fn[i], msg))
    }
    for (op in names(SESSION_INLOOP)) {
      msg <- tryCatch({ SESSION_INLOOP[[op]](cn, be); "OK" },
                      error = function(e) paste("FAIL:", conditionMessage(e)))
      cat(sprintf("  session   %-20s %s\n", op, msg))
    }
    try(datashield.logout(cn), silent = TRUE)
    msg <- tryCatch({
      lc <- datashield.login(login_for(logindata, be), assign = FALSE, restore = WS)
      try(datashield.logout(lc), silent = TRUE); "OK"
    }, error = function(e) paste("FAIL:", conditionMessage(e)))
    cat(sprintf("  session   %-20s %s\n", "login/workspace_load", msg))
    cat(sprintf("  -- %s: %d/%d primitives OK --\n", be, nok, nrow(PRIMITIVES)))
  }
  quit(save = "no")
}

# =============================================================================
# SUITE 1 -- function primitives (true compute + tight-poll round trip)
# =============================================================================
# One low-level submit -> tight poll -> read true server compute -> fetch.
# roundtrip_ms times only submit + poll + fetch. The compute probe is an extra
# network round trip (a DIFFERENT call per backend), so its wall-time is measured
# and subtracted back out -- it must not inflate or bias the comparison.
measure_true <- function(c1, kind, expr) {
  t0    <- Sys.time()
  res   <- submit_primitive(c1, kind, expr)
  repeat { if (dsIsCompleted(res)) break; Sys.sleep(TIGHT_POLL_SEC) }
  done  <- Sys.time()
  cms   <- command_compute_ms(c1, res)                          # read BEFORE fetch
  probe <- secs_since(done)                                     # cost of the probe, excluded below
  dsFetch(res)
  c(compute_ms = cms, roundtrip_ms = (secs_since(t0) - probe) * 1000)
}

cat("FUNCTION speed: ")
run_speed_suite(PRIMITIVES, BACKENDS, REPS, FUN_CSV,
                metrics = c("compute_ms", "roundtrip_ms"), measure = measure_true, node = TRUE)

# =============================================================================
# SUITE 2 -- session / DSI ops (client round trip only)
# =============================================================================
logindata <- build_logins(BACKENDS)
append_rows <- open_csv(SESS_CSV, c("backend", "op", "set", "rep", "roundtrip_ms"))
cat(sprintf("\nSESSION ops: %d in-loop + login/workspace_load, %d sets x %d reps x %d backends -> %s\n",
            length(SESSION_INLOOP), SETS, RPS, length(BACKENDS), SESS_CSV))

connect1 <- function(be) tryCatch(connect_be(be, logindata), error = function(e) {
  message(sprintf("  connect failed %s: %s", be, conditionMessage(e))); NULL })

# --- persistent-connection ops, interleaved by op x backend -----------------
conns <- list()
for (be in BACKENDS) { cn <- connect1(be); if (!is.null(cn)) conns[[be]] <- cn }
active  <- names(conns)
opnames <- names(SESSION_INLOOP)
set.seed(as.integer(Sys.getenv("SEED", "1")))   # after connect, so the shuffle is reproducible
# finally: log out on ANY exit (normal or error), so no server session lingers.
tryCatch(
  for (s in seq_len(SETS)) {
    cat(sprintf("\n-- session set %d/%d --\n", s, SETS))
    for (op in sample(opnames)) for (be in sample(active)) {
      f <- (function(op, be) function() SESSION_INLOOP[[op]](conns[[be]], be))(op, be)
      if (s == 1L) try(f(), silent = TRUE)                       # warm-up
      ms <- vapply(seq_len(RPS), function(r) tryCatch(timed(f),
        error = function(e) { conns[[be]] <<- connect1(be); NA_real_ }), numeric(1))
      append_rows(data.frame(backend = be, op = op, set = s,
                             rep = (s - 1L) * RPS + seq_len(RPS), roundtrip_ms = round(ms, 3)))
    }
  },
  finally = for (be in names(conns)) try(datashield.logout(conns[[be]]), silent = TRUE)
)

# --- login / workspace_load: a FRESH login each time (interleaved) -----------
# Only the login is on the clock; the logout that follows is untimed cleanup.
# login = plain fresh login; workspace_load = login restoring the saved workspace.
login_once <- function(be, op, clock) {
  s <- if (clock) Sys.time() else NULL
  cn <- if (op == "workspace_load")
          datashield.login(login_for(logindata, be), assign = FALSE, restore = WS)
        else datashield.login(login_for(logindata, be), assign = FALSE)
  t <- if (clock) secs_since(s) * 1000 else NA_real_
  try(datashield.logout(cn), silent = TRUE)   # untimed cleanup
  t
}
for (s in seq_len(SETS)) {
  cat(sprintf("\n-- fresh-login set %d/%d --\n", s, SETS))
  for (op in sample(c("login", "workspace_load"))) for (be in sample(active)) {
    if (s == 1L) try(login_once(be, op, FALSE), silent = TRUE)   # warm-up (untimed)
    ms <- vapply(seq_len(RPS), function(r) tryCatch(login_once(be, op, TRUE),
                 error = function(e) NA_real_), numeric(1))
    append_rows(data.frame(backend = be, op = op, set = s,
                           rep = (s - 1L) * RPS + seq_len(RPS), roundtrip_ms = round(ms, 3)))
  }
}

cat(sprintf("\nWrote %s\n", SESS_CSV))
