# ==============================================================================
# DSI / session-op round trip. These orchestration ops are NOT single dsBase
# commands -- there is no server compute clock to read -- so we time only the
# client round trip on every backend, fair-interleaved (same shuffled-sets
# structure as speed_true.R). compute is N/A; overhead and network are still
# separable downstream via the localhost baseline (overhead = localhost round
# trip, network = remote - localhost).
#
#   SPEED_REPS=20 ARMA_AUTH=basic Rscript speed_dsi.R
# Output: results/speed_dsi.csv  (backend, op, set, rep, roundtrip_ms)
# ==============================================================================

source("bench_lib.R")
REPS  <- as.integer(Sys.getenv("SPEED_REPS", "4"))   # reps per pass
SETS  <- max(1L, as.integer(Sys.getenv("SPEED_SETS", "5")))
RPS   <- max(1L, REPS)
TOTAL <- SETS * RPS
OUT   <- Sys.getenv("SPEED_DSI_CSV", file.path(dirname(OUT_CSV), "speed_dsi.csv"))
WS    <- "benchws"

logindata <- build_logins(BACKENDS)
timed <- function(f) { s <- Sys.time(); f(); secs_since(s) * 1000 }

# Orchestration ops timed on a live connection (round trip only).
INLOOP <- list(
  tables         = function(cn, be) datashield.tables(cn),
  profiles       = function(cn, be) datashield.profiles(cn),
  workspaces     = function(cn, be) datashield.workspaces(cn),
  pkg_status     = function(cn, be) datashield.pkg_status(cn),
  assign.table   = function(cn, be) datashield.assign.table(cn, "dsi_tmp", table_a_ref(be)),
  workspace_save = function(cn, be) datashield.workspace_save(cn, WS)
)

append_rows <- open_csv(OUT, c("backend", "op", "set", "rep", "roundtrip_ms"), append = FALSE)
cat(sprintf("DSI ops: %d in-loop + login/workspace_load, %d sets x %d reps x %d backends -> %s\n",
            length(INLOOP), SETS, RPS, length(BACKENDS), OUT))

connect1 <- function(be) tryCatch(connect_be(be, logindata), error = function(e) {
  message(sprintf("  connect failed %s: %s", be, conditionMessage(e))); NULL })

# --- persistent-connection ops, interleaved by op x backend -----------------
conns <- list()
for (be in BACKENDS) { cn <- connect1(be); if (!is.null(cn)) conns[[be]] <- cn }
active  <- names(conns)
opnames <- names(INLOOP)
set.seed(as.integer(Sys.getenv("SEED", "1")))   # after connect, so the shuffle is reproducible
for (s in seq_len(SETS)) {
  cat(sprintf("\n-- dsi set %d/%d --\n", s, SETS))
  for (op in sample(opnames)) for (be in sample(active)) {
    f <- (function(op, be) function() INLOOP[[op]](conns[[be]], be))(op, be)
    if (s == 1L) try(f(), silent = TRUE)                       # warm-up
    ms <- vapply(seq_len(RPS), function(r) tryCatch(timed(f),
      error = function(e) { conns[[be]] <<- connect1(be); NA_real_ }), numeric(1))
    append_rows(data.frame(backend = be, op = op, set = s,
                           rep = (s - 1L) * RPS + seq_len(RPS), roundtrip_ms = round(ms, 3)))
  }
}
for (be in active) try(datashield.logout(conns[[be]]), silent = TRUE)

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

cat(sprintf("\nWrote %s\n", OUT))
