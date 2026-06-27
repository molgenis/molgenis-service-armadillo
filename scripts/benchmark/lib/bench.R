# ==============================================================================
# Broad throughput survey: full ds.*/datashield.* function calls, ops/sec, with
# the DSI poll-sleep lowered so the client-side wait doesn't dominate fast ops:
#
#   POLL_SLEEP0=0.002 Rscript bench.R         # -> results/rates.csv
#   DURATION_SEC=2 REPS=1 POLL_SLEEP0=0.002 Rscript bench.R   # quick smoke run
#
# Each (backend x op x rep) cell runs the op for DURATION_SEC, counting completed
# calls; reps are independent shuffled blocks. Results are written incrementally
# to results/rates.csv (failures to results/failures.csv). A dropped backend is
# healed (Opal auto-restarted if OPAL_COMPOSE is set) and retried; one that
# cannot be healed is skipped and re-probed next repetition.
# Output columns: backend, op, category, rep, count, elapsed, rate
#
# Sibling scripts (all sourcing bench_lib.R): ops.R (the registry), probe.R
# (validate call forms, no timing), capture.R (extract primitives.csv),
# speed_true.R / speed_client.R (true vs client time of those primitives).
# ==============================================================================

source("bench_lib.R")          # config.R + shared helpers
source("ops.R")                # ds_ops registry + flatten_ops + PREP_FOR
set.seed(SEED)

logindata <- build_logins()
conns     <- build_conns(BACKENDS, logindata)
if (!length(conns)) stop("No backends available; nothing to benchmark.")

# --- Session timing (login / logout / workspace_load) -----------------------
# Throwaway connections so it doesn't disturb the persistent ones. Returns rows
# tagged category "session".
session_rows <- function(be, rep) {
  ld <- login_for(logindata, be)

  n <- 0L; lt <- 0; ot <- 0; t0 <- Sys.time()
  repeat {
    s <- Sys.time(); cn <- datashield.login(ld, assign = FALSE); lt <- lt + secs_since(s)
    s <- Sys.time(); datashield.logout(cn);                       ot <- ot + secs_since(s)
    n <- n + 1L
    if (secs_since(t0) >= DURATION_SEC) break
  }

  m <- 0L; wt <- 0; t0 <- Sys.time()
  repeat {
    s <- Sys.time(); cn <- datashield.login(ld, assign = FALSE, restore = WORKSPACE); wt <- wt + secs_since(s)
    datashield.logout(cn)
    m <- m + 1L
    if (secs_since(t0) >= DURATION_SEC) break
  }

  data.frame(
    backend = be, op = c("login", "logout", "workspace_load"), category = "session",
    rep = rep, count = c(n, n, m), elapsed = c(lt, ot, wt),
    rate = c(n / lt, n / ot, m / wt)
  )
}

# --- Build cells (each carries its identity), shuffled per repetition --------
# A cell's run(): resolve the LIVE connection -> reset workspace -> set default
# connection -> build prerequisite (untimed) -> one untimed warm-up call
# (exclude cold-start) -> timed loop.
make_cell <- function(be, op, category, rep, fn) {
  force(be); force(op); force(category); force(rep); force(fn)  # avoid lazy capture
  list(
    be = be, op = op, rep = rep,
    run = function() {
      cn <- conns[[be]]
      reset(cn)
      use_conn(cn)
      if (!is.null(PREP_FOR[[op]])) try(PREP_FOR[[op]](cn), silent = TRUE)
      try(fn(cn), silent = TRUE)           # warm-up, excluded from timing
      r <- time_op(function() fn(cn), DURATION_SEC)
      data.frame(backend = be, op = op, category = category, rep = rep, r)
    })
}

cells <- list()
for (rep in seq_len(REPS)) {
  rep_cells <- list()
  for (be in names(conns)) {
    for (o in flatten_ops(be))
      rep_cells[[length(rep_cells) + 1]] <- make_cell(be, o$op, o$category, rep, o$fn)
    local({
      be_ <- be; rep_ <- rep
      rep_cells[[length(rep_cells) + 1]] <<- list(be = be_, op = "session", rep = rep_,
        run = function() session_rows(be_, rep_))
    })
  }
  cells <- c(cells, rep_cells[sample(length(rep_cells))])   # fresh shuffle per rep
}

cat(sprintf("Running %d cells (%g s each), order reshuffled per repetition...\n",
            length(cells), DURATION_SEC))

backend_alive <- function(be)
  tryCatch({ ds.ls(datasources = conns[[be]]); TRUE }, error = function(e) FALSE)

# Heal a dropped backend: restart Opal's container (if OPAL_COMPOSE set), then
# reconnect, waiting up to ~2 min. Updates the global `conns`.
heal <- function() {
  for (be in names(conns)) {
    if (backend_alive(be)) next
    message(sprintf("  %s connection lost; healing...", be))
    if (be == "opal" && nzchar(OPAL_COMPOSE)) {
      message("  restarting Opal container (docker compose up -d)...")
      try(system2("docker", c("compose", "-f", OPAL_COMPOSE, "up", "-d"),
                  stdout = FALSE, stderr = FALSE), silent = TRUE)
    }
    newcn <- NULL
    for (k in seq_len(24)) {                  # wait up to ~2 min for it to recover
      newcn <- tryCatch(connect_be(be, logindata), error = function(e) NULL)
      if (!is.null(newcn)) break
      Sys.sleep(5)
    }
    if (!is.null(newcn)) { conns[[be]] <<- newcn; message(sprintf("  %s reconnected", be)) }
    else message(sprintf("  %s still unavailable", be))
  }
}

# Run a cell's closure, capturing result, error message, and warnings.
run_cell <- function(run) {
  warns <- character(0); err <- NA_character_
  res <- withCallingHandlers(
    tryCatch(run(), error = function(e) { err <<- conditionMessage(e); NULL }),
    warning = function(w) { warns[[length(warns) + 1L]] <<- conditionMessage(w); invokeRestart("muffleWarning") })
  if (length(warns)) {
    counts <- table(warns)
    for (m in names(counts)) message(sprintf("  warning (x%d): %s", counts[[m]], m))
  }
  list(res = res, err = err)
}

# Results written incrementally (a crash never loses completed cells); failures
# recorded to a sibling file so missing rows are explained, not silent.
COLS      <- c("backend", "op", "category", "rep", "count", "elapsed", "rate")
FAIL_CSV  <- file.path(dirname(OUT_CSV), sub("^rates", "failures", basename(OUT_CSV)))
append_res  <- open_csv(OUT_CSV,  COLS)
append_fail <- open_csv(FAIL_CSV, c("backend", "op", "rep", "error"))
log_fail <- function(be, op, rep, msg)
  append_fail(data.frame(backend = be, op = op, rep = rep, error = gsub("[\r\n,]", " ", msg)))

# Pass-based work queue so a crash misses no measurements. Each pass runs every
# cell whose backend is currently up; a cell whose backend is DOWN is deferred
# (not the op's fault) and a genuine op failure on a live backend is logged once.
# Between passes the down backends are healed (Opal restarted), and deferred
# cells are retried next pass -- so a crashed-then-recovered backend still gets
# every (op x rep) measured. A backend down for MAX_FRUITLESS_PASSES in a row is
# given up on (its cells logged "backend unavailable") so the run can't hang.
MAX_FRUITLESS_PASSES <- 5L
HEAL_WAIT <- 20L                                    # seconds between fruitless passes
be_of <- function(x) vapply(x, function(c) c$be, character(1))

pending <- cells
total <- length(cells)
n_written <- 0L; n_failed <- 0L; done <- 0L; fruitless <- 0L
while (length(pending)) {
  down <- character(0)                              # backends found down this pass
  next_pending <- list()
  progressed <- FALSE
  for (cl in pending) {
    if (cl$be %in% down) { next_pending[[length(next_pending) + 1L]] <- cl; next }
    out <- run_cell(cl$run)
    if (!is.null(out$res)) {
      append_res(out$res); n_written <- n_written + nrow(out$res); done <- done + 1L; progressed <- TRUE
    } else if (!backend_alive(cl$be)) {             # backend down -> defer, don't blame the op
      down <- union(down, cl$be)
      next_pending[[length(next_pending) + 1L]] <- cl
    } else {                                        # genuine op failure on a live backend
      log_fail(cl$be, cl$op, cl$rep, if (is.na(out$err)) "unknown error" else out$err)
      n_failed <- n_failed + 1L; done <- done + 1L
    }
    cat(sprintf("[%d/%d]\n", done, total))
  }
  if (!length(next_pending)) break
  message(sprintf("  %d cell(s) deferred on down backend(s): %s; healing...",
                  length(next_pending), paste(unique(be_of(next_pending)), collapse = ", ")))
  heal()
  recovered <- any(vapply(unique(be_of(next_pending)), backend_alive, logical(1)))
  fruitless <- if (progressed || recovered) 0L else fruitless + 1L
  if (fruitless >= MAX_FRUITLESS_PASSES) {
    for (cl in next_pending) {
      log_fail(cl$be, cl$op, cl$rep, "backend unavailable after retries"); n_failed <- n_failed + 1L; done <- done + 1L
    }
    message("  giving up on persistently-down backend(s) after retries"); break
  }
  if (!progressed && !recovered) Sys.sleep(HEAL_WAIT)
  pending <- next_pending
}

logout_all(conns)
cat(sprintf("\nWrote %d result rows -> %s\n%d failures -> %s\n",
            n_written, OUT_CSV, n_failed, FAIL_CSV))
