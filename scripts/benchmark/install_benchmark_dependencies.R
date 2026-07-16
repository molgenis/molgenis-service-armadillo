#!/usr/bin/env Rscript
# ==============================================================================
# Install the benchmark's R dependencies into a project-local library (.Rlib by
# default; override with BENCH_LIB).
#
#   ./install_benchmark_dependencies.R
#
# These are CLIENT + tooling packages only. The benchmark submits raw serverside
# calls (see PRIMITIVES in lib/helpers.R), so the dsBase that actually computes
# lives in the server profiles -- nothing dsBase is installed client-side.
# ==============================================================================

LOCAL_LIB <- Sys.getenv("BENCH_LIB", ".Rlib")
dir.create(LOCAL_LIB, showWarnings = FALSE, recursive = TRUE)
.libPaths(c(normalizePath(LOCAL_LIB), .libPaths()))

CRAN <- "https://cloud.r-project.org"

pkgs <- c(
  "DSI", "DSOpal", "DSMolgenisArmadillo",   # DataSHIELD client drivers
  "opalr", "MolgenisArmadillo", "tibble",   # setup.R: table upload
  "ggplot2", "ragg",                        # plots.R: figures
  "httr"                                     # helpers.R: command timing
)

for (p in pkgs) {
  if (requireNamespace(p, quietly = TRUE, lib.loc = .libPaths())) {
    message(sprintf("  [skip]    %s", p))
  } else {
    message(sprintf("  [install] %s", p))
    install.packages(p, lib = LOCAL_LIB, repos = CRAN, dependencies = TRUE)
  }
}
message("Done.")
