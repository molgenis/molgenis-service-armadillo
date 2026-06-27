# ==============================================================================
# Chart the benchmark results relative to Opal. Reads results/rates.csv and, for
# each op, shows how many times faster/slower every OTHER backend is than Opal
# (mean ops/sec across reps). Opal is the 1x baseline (the 0 line); bars to the
# right mean faster than Opal, to the left mean slower.
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

opal   <- rate_of("opal")
others <- setdiff(BACKENDS, "opal")

# Signed fold-change vs Opal, per op, for each non-Opal backend:
#   +N => N x faster than Opal;  -N => N x slower than Opal.
rows <- list()
for (be in others) {
  r   <- rate_of(be)
  ops <- intersect(names(opal), names(r))
  if (!length(ops)) next
  fold <- ifelse(r[ops] >= opal[ops], r[ops] / opal[ops], -(opal[ops] / r[ops]))
  rows[[be]] <- data.frame(op = ops, backend = be, fold = as.numeric(fold), row.names = NULL)
}
cmp <- do.call(rbind, rows)
stopifnot(!is.null(cmp), nrow(cmp) > 0)
cmp$backend <- factor(cmp$backend, levels = others)

# Order ops by mean advantage; note any op not measured on all backends.
op_order <- names(sort(tapply(cmp$fold, cmp$op, mean)))
cmp$op   <- factor(cmp$op, levels = op_order)
common   <- Reduce(intersect, lapply(BACKENDS, function(be) names(rate_of(be))))
dropped  <- setdiff(unique(m$op), common)
cap <- if (length(dropped))
  paste("Not measured on every backend:", paste(sort(dropped), collapse = ", ")) else NULL

lim  <- max(abs(cmp$fold)) * 1.05
maxb <- ceiling(lim / 5) * 5
brks  <- seq(-maxb, maxb, by = 5)     # major ticks every 5×
minbr <- seq(-maxb, maxb, by = 2.5)   # minor gridlines at the 2.5× midpoints
PLOT <- file.path(dirname(OUT_CSV), "comparison.png")
p <- ggplot(cmp, aes(x = op, y = fold, fill = backend)) +
  geom_col(width = 0.7, position = position_dodge(width = 0.8)) +
  geom_hline(yintercept = 0, linewidth = 0.4) +
  coord_flip() +
  scale_y_continuous(limits = c(-lim, lim), breaks = brks, minor_breaks = minbr,
                     labels = function(x) sprintf("%g×", abs(x))) +
  scale_fill_manual(values = c(armadillo = "#00BFC4", armadillo_rserve = "#C77CFF")) +
  labs(title = "Throughput relative to Opal, per op",
       subtitle = sprintf("× faster / slower than Opal (mean ops/sec, %d rep(s)); 0 = parity",
                          length(unique(d$rep))),
       x = NULL,
       y = "← slower than Opal        faster than Opal →",
       fill = "vs Opal", caption = cap) +
  theme_minimal(base_size = 12)

ggsave(PLOT, p, width = 9, height = 6, dpi = 150)
cat(sprintf("Wrote %s\n", PLOT))
