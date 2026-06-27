# ==============================================================================
# SUITE 1 -- TRUE server speed. For each extracted single-command primitive
# (results/primitives.csv) on each backend, measure the server's own execution
# time (endDate - startDate) plus a tight-poll round trip. No 50ms poll-sleep,
# so we can afford many reps.
#
#   ARMA_AUTH=basic Rscript capture.R             # (once) build primitives.csv
#   SPEED_REPS=1000 ARMA_AUTH=basic Rscript speed_true.R
#
# Output: results/speed_true.csv  (backend, pid, fn, kind, rep, compute_ms, roundtrip_ms)
# ==============================================================================

source("bench_lib.R")
REPS <- as.integer(Sys.getenv("SPEED_REPS", "1000"))
OUT  <- Sys.getenv("SPEED_TRUE_CSV", file.path(dirname(OUT_CSV), "speed_true.csv"))

# One low-level submit -> tight poll -> read true server compute -> fetch.
measure_true <- function(c1, kind, expr) {
  t0  <- Sys.time()
  res <- submit_primitive(c1, kind, expr)
  repeat { if (dsIsCompleted(res)) break; Sys.sleep(TIGHT_POLL_SEC) }
  cms <- command_compute_ms(c1, res)                            # read BEFORE fetch
  dsFetch(res)
  c(compute_ms = cms, roundtrip_ms = secs_since(t0) * 1000)
}

cat("TRUE speed: ")
run_speed_suite(read_primitives(), build_conns(), REPS, OUT,
                metrics = c("compute_ms", "roundtrip_ms"), measure = measure_true, node = TRUE)
