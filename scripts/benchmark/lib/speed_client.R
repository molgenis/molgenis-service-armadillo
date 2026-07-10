# ==============================================================================
# SUITE 2 -- CLIENT poll penalty. The SAME extracted primitives
# (results/primitives.csv), each timed TWICE back-to-back through the identical
# high-level DSI call (datashield.aggregate / datashield.assign.expr), differing
# ONLY in the async poll interval:
#   tight_ms  -- datashield.polling.sleep.0 = TIGHT_POLL_SEC (2 ms; never 0, which
#                would busy-hammer the server)
#   client_ms -- the DSI default 50 ms poll: the real end-user experience
# Because both runs share the same path in the same pass, the per-rep poll penalty
# client_ms - tight_ms cancels the shared compute + transport and isolates the
# poll-sleep. Directly comparable to speed_true.R's compute_ms.
#
#   SPEED_REPS=20 ARMA_AUTH=basic Rscript speed_client.R
#
# Output: results/speed_client.csv  (backend, pid, fn, kind, rep, tight_ms, client_ms)
# ==============================================================================

source("bench_lib.R")
REPS <- as.integer(Sys.getenv("SPEED_REPS", "100"))
OUT  <- Sys.getenv("SPEED_CLIENT_CSV", file.path(dirname(OUT_CSV), "speed_client.csv"))

# The same high-level call, timed at a tight poll then at the DSI default poll.
# DSI's poll loop reads getOption("datashield.polling.sleep.0") each call, so
# setting it switches the interval; sleep.1 is set too so long ops never escalate
# mid-run. Both intervals come from .env (SPEED_POLL_TIGHT / SPEED_POLL_CLIENT):
# the client run sets its poll EXPLICITLY because config.R may have lowered the
# global poll via POLL_SLEEP0, so restoring the prior value would NOT give the
# intended end-user default.
measure_poll <- function(cn, kind, expr) {
  old <- options(datashield.polling.sleep.0 = TIGHT_POLL_SEC,
                 datashield.polling.sleep.1 = TIGHT_POLL_SEC)
  s <- Sys.time(); run_primitive_hl(cn, kind, expr); tight_ms  <- secs_since(s) * 1000
  options(datashield.polling.sleep.0 = SPEED_POLL_CLIENT, datashield.polling.sleep.1 = 1)  # end-user default
  s <- Sys.time(); run_primitive_hl(cn, kind, expr); client_ms <- secs_since(s) * 1000
  options(old)
  c(tight_ms = tight_ms, client_ms = client_ms)
}

cat("CLIENT poll: ")
run_speed_suite(read_primitives(), BACKENDS, REPS, OUT,
                metrics = c("tight_ms", "client_ms"), measure = measure_poll, node = FALSE)
