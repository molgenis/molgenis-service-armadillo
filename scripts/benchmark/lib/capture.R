# ==============================================================================
# Extract the single-command serverside primitives the broad op set issues, into
# results/primitives.csv (the shared contract speed_true.R / speed_client.R read).
#
#   ARMA_AUTH=basic Rscript capture.R
#
# Every op resolves to one or more datashield.aggregate / datashield.assign.expr
# calls; each is a single-command primitive. write_primitives() traces them
# (contained + always untraced there), dedups, validates each standalone, and
# writes results/primitives.csv (kind, fn, expr). Regenerate-only: run it once
# after the op set or servers change; the speed scripts then reuse the CSV.
# ==============================================================================

source("bench_lib.R")          # config.R + shared helpers (incl. write_primitives)
source("ops.R")                # ds_ops registry + flatten_ops

conns <- build_conns()
if (!length(conns)) stop("No backends available.")

# Capture/validate on a stable backend (Opal is OOM-prone); the serverside call
# expressions are backend-independent, so one backend suffices.
be  <- if ("armadillo" %in% names(conns)) "armadillo" else names(conns)[1]
cat(sprintf("Capturing serverside primitives on '%s'...\n", be))
out <- write_primitives(conns[[be]], flatten_ops(be))

logout_all(conns)
cat(sprintf("\n-- %d standalone single-command primitives -> %s --\n", nrow(out), PRIM_CSV))
