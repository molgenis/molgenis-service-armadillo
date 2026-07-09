# ==============================================================================
# Chart the benchmark results, SPLIT into localhost vs remote. For each op, show
# how many times faster/slower each Armadillo profile is than Opal *in the same
# environment* (mean ops/sec across reps). One panel per environment; Opal is the
# 1x baseline (0 line), bars right = faster than that env's Opal, left = slower.
#
#   Rscript plot.R
#
# Requires ggplot2 (in addition to the benchmark prerequisites).
# Output: results/comparison.png
# ==============================================================================

source("config.R")
suppressMessages(library(ggplot2))

stopifnot(file.exists(OUT_CSV))
d <- read.csv(OUT_CSV, stringsAsFactors = FALSE)

# Mean rate per (backend, op); helper to pull one backend's op -> rate vector.
m <- aggregate(rate ~ backend + op, data = d, FUN = mean)
rate_of <- function(be) setNames(m$rate[m$backend == be], m$op[m$backend == be])

# Compare each environment's Armadillo profiles to THAT environment's Opal.
locs <- intersect(c("local", "remote"), unique(vapply(BACKENDS, backend_location, character(1))))
rows <- list()
for (loc in locs) {
  base <- rate_of(paste0("opal_", loc))
  if (!length(base)) next
  arma_bes <- BACKENDS[backend_kind(BACKENDS) == "armadillo" &
                       vapply(BACKENDS, backend_location, character(1)) == loc]
  for (be in arma_bes) {
    r   <- rate_of(be)
    ops <- intersect(names(base), names(r))
    if (!length(ops)) next
    fold    <- ifelse(r[ops] >= base[ops], r[ops] / base[ops], -(base[ops] / r[ops]))
    profile <- if (grepl("_rserve$", be)) "rserve" else "default"
    rows[[be]] <- data.frame(op = ops, location = loc, profile = profile,
                             fold = as.numeric(fold), row.names = NULL)
  }
}
cmp <- do.call(rbind, rows)
stopifnot(!is.null(cmp), nrow(cmp) > 0)

# Order ops by overall mean advantage; consistent across panels.
op_order     <- names(sort(tapply(cmp$fold, cmp$op, mean)))
cmp$op       <- factor(cmp$op, levels = op_order)
cmp$location <- factor(cmp$location, levels = locs)
cmp$profile  <- factor(cmp$profile, levels = intersect(c("default", "rserve"), unique(cmp$profile)))

lim  <- max(abs(cmp$fold)) * 1.05
PLOT <- file.path(dirname(OUT_CSV), "comparison.png")
env_labels <- c(local = "localhost", remote = "remote")
p <- ggplot(cmp, aes(x = op, y = fold, fill = profile)) +
  geom_col(width = 0.7, position = position_dodge(width = 0.8)) +
  geom_hline(yintercept = 0, linewidth = 0.4) +
  coord_flip() +
  facet_wrap(~ location, labeller = labeller(location = env_labels)) +
  scale_y_continuous(limits = c(-lim, lim), labels = function(x) sprintf("%g×", abs(x))) +
  scale_fill_manual(values = c(default = "#00BFC4", rserve = "#C77CFF")) +
  labs(title = "Armadillo throughput relative to Opal, per op",
       subtitle = sprintf("× faster / slower than Opal in the SAME environment (mean ops/sec, %d rep(s)); 0 = parity",
                          length(unique(d$rep))),
       x = NULL,
       y = "← slower than Opal        faster than Opal →",
       fill = "Armadillo profile") +
  theme_minimal(base_size = 12)

ggsave(PLOT, p, width = 11, height = 6, dpi = 150)
cat(sprintf("Wrote %s\n", PLOT))
