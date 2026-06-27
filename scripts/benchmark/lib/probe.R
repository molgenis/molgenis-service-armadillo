# ==============================================================================
# Validate every call form in the op registry: run each op ONCE per backend and
# report OK/FAIL (no timing). Use it before a timed `bench.R` run to catch
# call-form / version-skew problems fast.
#
#   ARMA_AUTH=basic Rscript probe.R
# ==============================================================================

source("bench_lib.R")          # config.R + shared helpers
source("ops.R")                # ds_ops registry + flatten_ops + PREP_FOR

conns <- build_conns()
if (!length(conns)) stop("No backends available.")

for (be in names(conns)) {
  cn  <- conns[[be]]
  ops <- flatten_ops(be)
  cat(sprintf("\n== PROBE %s (%d ops) ==\n", be, length(ops)))
  nok <- 0L
  for (o in ops) {
    reset(cn)
    use_conn(cn)
    if (!is.null(PREP_FOR[[o$op]])) try(PREP_FOR[[o$op]](cn), silent = TRUE)
    msg <- tryCatch({ o$fn(cn); "OK" }, error = function(e) paste("FAIL:", conditionMessage(e)))
    if (identical(msg, "OK")) nok <- nok + 1L
    cat(sprintf("  %-28s %s\n", o$op, msg))
  }
  cat(sprintf("  -- %s: %d/%d OK, %d FAIL --\n", be, nok, length(ops), length(ops) - nok))
}

logout_all(conns)
