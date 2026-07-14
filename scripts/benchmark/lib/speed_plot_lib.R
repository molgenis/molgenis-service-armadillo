# ==============================================================================
# Shared helpers for the diverging log2 "x vs Opal" speed figures used by
# plot_total.R (per function family) and plot_dsi.R (per session op).
# Pure ggplot + data helpers -- no config.R dependency.
# ==============================================================================
suppressMessages(library(ggplot2))

# backend name -> location / engine
speed_location <- function(backend) ifelse(grepl("remote", backend), "remote", "local")
speed_engine <- function(backend)
  ifelse(grepl("^opal", backend), "Opal",
  ifelse(grepl("rserve", backend), "Armadillo-Rserve", "Armadillo-Rock"))

# log2 fold-change axis: "Opal" (parity) at 0, a doubling per step either side.
SPEED_BRK   <- -4:4
SPEED_LBL   <- c("16x", "8x", "4x", "2x", "Opal", "2x", "4x", "8x", "16x")
ENGINE_FILL <- c("Armadillo-Rock" = "#4285F4", "Armadillo-Rserve" = "#0097A7")

# median + IQR of `lfc` grouped by `by` (e.g. "family" or "op") x location x engine.
# All three stats come from one aggregate() pass, so rows stay aligned.
speed_agg <- function(df, by) {
  f <- reformulate(c(by, "location", "engine"), response = "lfc")
  a <- aggregate(f, df, function(x) c(
    m  = median(x, na.rm = TRUE),
    lo = as.numeric(quantile(x, 0.25, na.rm = TRUE, names = FALSE)),
    hi = as.numeric(quantile(x, 0.75, na.rm = TRUE, names = FALSE))))
  s <- as.data.frame(a$lfc)
  a$lfc <- s$m; a$lo <- s$lo; a$hi <- s$hi
  a
}

# one diverging log2 figure for a single-location subset of a speed_agg() frame.
speed_plot <- function(sub, xvar, limits, title) {
  ggplot(sub, aes(x = .data[[xvar]], y = lfc, fill = engine)) +
    geom_col(position = position_dodge(width = 0.7), width = 0.62) +
    geom_errorbar(aes(ymin = lo, ymax = hi), position = position_dodge(width = 0.7),
                  width = 0.3, linewidth = 0.3) +
    geom_hline(yintercept = 0, colour = "#888") +
    coord_flip() +
    scale_y_continuous(breaks = SPEED_BRK, labels = SPEED_LBL, limits = limits) +
    scale_fill_manual(values = ENGINE_FILL) +
    labs(title = title, x = NULL, y = NULL, fill = NULL) +
    theme_minimal(base_size = 13, base_family = "IBM Plex Mono") +
    theme(legend.position = "none", plot.margin = margin(10, 14, 5, 8),
          plot.title = element_text(size = 12),
          axis.text.y = element_text(hjust = 0),
          panel.grid.minor = element_blank())
}

# render localhost + remote PNGs into <out_dir>/<prefix>_{local,remote}.png,
# sized to match the footprint charts so they line up on the slide.
speed_save <- function(agg, xvar, limits, out_dir, prefix) {
  for (loc in c("localhost", "remote")) {
    p <- speed_plot(agg[agg$location == loc, ], xvar, limits, tools::toTitleCase(loc))
    f <- file.path(out_dir, sprintf("%s_%s.png", prefix, if (loc == "localhost") "local" else "remote"))
    ggsave(f, p, width = 6.6, height = 4.2, dpi = 300, device = ragg::agg_png)
    cat(sprintf("Wrote %s\n", f))
  }
}
