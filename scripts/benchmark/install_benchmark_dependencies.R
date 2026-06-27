#!/usr/bin/env Rscript
# ==============================================================================
# Install the benchmark's R dependencies into a project-local library (.Rlib by
# default; override with BENCH_LIB). dsBaseClient comes from the Obiba CRAN at the
# version your servers run -- it MUST match DSBASE_VERSION (the dsBase the Rock
# images are built with) or some functions fail with version-skew errors.
#
#   DSBASE_VERSION=6.3.5 ./install_benchmark_dependencies.R
# ==============================================================================

LOCAL_LIB <- Sys.getenv("BENCH_LIB", ".Rlib")
dir.create(LOCAL_LIB, showWarnings = FALSE, recursive = TRUE)
.libPaths(c(normalizePath(LOCAL_LIB), .libPaths()))

CRAN  <- "https://cloud.r-project.org"
OBIBA <- "https://cran.obiba.org"

# Client driver stack, server admin clients, plotting + http helpers.
cran_pkgs <- c("DSI", "DSOpal", "DSMolgenisArmadillo", "opalr", "MolgenisArmadillo",
               "tibble", "ggplot2", "httr", "jsonlite", "remotes")

install_missing <- function(pkgs, repos) {
  for (p in pkgs) {
    if (requireNamespace(p, quietly = TRUE, lib.loc = .libPaths())) {
      message(sprintf("  [skip] %s already installed", p))
    } else {
      message(sprintf("  [install] %s", p))
      install.packages(p, lib = LOCAL_LIB, repos = repos, dependencies = TRUE)
    }
  }
}

message("Installing CRAN dependencies into ", LOCAL_LIB, " ...")
install_missing(cran_pkgs, CRAN)

# dsBaseClient: the Obiba release. Pin matters -- warn loudly on mismatch.
DSBASE_VERSION <- Sys.getenv("DSBASE_VERSION", "")
message("Installing dsBaseClient from ", OBIBA, " ...")
install.packages("dsBaseClient", lib = LOCAL_LIB, repos = OBIBA, dependencies = FALSE)

got <- as.character(packageVersion("dsBaseClient", lib.loc = LOCAL_LIB))
if (nzchar(DSBASE_VERSION) && !startsWith(got, DSBASE_VERSION)) {
  warning(sprintf(paste0("Installed dsBaseClient %s does NOT match DSBASE_VERSION=%s. ",
    "The Obiba CRAN serves the current release; if it differs from your servers' ",
    "dsBase, comparison results will be invalid. Install a matching version manually."),
    got, DSBASE_VERSION), call. = FALSE, immediate. = TRUE)
} else {
  message(sprintf("dsBaseClient %s installed.", got))
}
message("Done.")
