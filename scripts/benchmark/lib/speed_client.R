# ==============================================================================
# SUITE 2 -- CLIENT speed (end-user experience). The SAME extracted primitives
# (results/primitives.csv), but timed as the client observes them: the high-level
# datashield.aggregate / datashield.assign.expr call through the DSI async poll
# loop (default 50ms poll-sleep). Directly comparable to speed_true.R. Slower per
# call, so fewer reps.
#
#   SPEED_REPS=100 ARMA_AUTH=basic Rscript speed_client.R
#
# Output: results/speed_client.csv  (backend, pid, fn, kind, rep, client_ms)
# ==============================================================================

source("bench_lib.R")
REPS <- as.integer(Sys.getenv("SPEED_REPS", "100"))
OUT  <- Sys.getenv("SPEED_CLIENT_CSV", file.path(dirname(OUT_CSV), "speed_client.csv"))

# One high-level call through the DSI async poll loop (default poll-sleep).
measure_client <- function(cn, kind, expr) {
  s <- Sys.time()
  run_primitive_hl(cn, kind, expr)
  c(client_ms = secs_since(s) * 1000)
}

cat("CLIENT speed: ")
run_speed_suite(read_primitives(), build_conns(), REPS, OUT,
                metrics = "client_ms", measure = measure_client, node = FALSE)
