# ==============================================================================
# Session & I/O ops (from speed_dsi.csv), vs Opal, by operation. Same spirit as
# total.png: Opal is the parity reference, Rock + Rserve engines, localhost vs
# remote facets. But session ops go BOTH ways (Armadillo faster on assign/save,
# much slower on login/workspace_load), so this is a diverging log2 fold-change:
#   right = Armadillo faster, left = slower, 0 = Opal parity. IQR whiskers.
# Output: results/session_dsi.png
#   Rscript speed_dsi.R && Rscript plot_dsi.R
# ==============================================================================

source("config.R")
suppressMessages(library(ggplot2))

DSI_CSV <- Sys.getenv("SPEED_DSI_CSV", file.path(dirname(OUT_CSV), "speed_dsi.csv"))
stopifnot(file.exists(DSI_CSV))
d <- read.csv(DSI_CSV, stringsAsFactors = FALSE)
d <- d[!is.na(d$roundtrip_ms) & d$roundtrip_ms > 0, ]
d$location <- ifelse(grepl("remote", d$backend), "remote", "local")
d$engine   <- ifelse(grepl("^opal", d$backend), "Opal",
              ifelse(grepl("rserve", d$backend), "Armadillo-Rserve", "Armadillo-Rock"))

# Opal reference: median round-trip per op x location, then log2 fold per arma rep
opal <- aggregate(roundtrip_ms ~ op + location, d[d$engine == "Opal", ], median)
names(opal)[3] <- "opal_rt"
a <- merge(d[d$engine != "Opal", ], opal, by = c("op", "location"))
a$lfc <- log2(a$opal_rt / a$roundtrip_ms)          # >0 Armadillo faster, <0 slower

q <- function(x, p) as.numeric(quantile(x, p, na.rm = TRUE, names = FALSE))
agg    <- aggregate(lfc ~ op + location + engine, a, median)
agg$lo <- aggregate(lfc ~ op + location + engine, a, q, 0.25)$lfc
agg$hi <- aggregate(lfc ~ op + location + engine, a, q, 0.75)$lfc

ord <- aggregate(lfc ~ op, agg, median)            # order worst -> best
agg$op       <- factor(agg$op, levels = ord$op[order(ord$lfc)])
agg$location <- factor(agg$location, levels = c("local", "remote"), labels = c("localhost", "remote"))

# Symmetric x-range so 0 (Opal parity) sits dead-centre, with padding either side.
lim <- max(abs(c(agg$lo, agg$hi)), na.rm = TRUE) * 1.06
brk <- -4:4
lbl <- c("16x", "8x", "4x", "2x", "Opal", "2x", "4x", "8x", "16x")   # |fold|; left slower, right faster
# One self-contained figure per location, to sit side-by-side on the slide like
# the memory/storage pair. Shared axis (lim) keeps them comparable; legend on slide.
mk <- function(loc, file) {
  sub <- agg[agg$location == loc, ]
  p <- ggplot(sub, aes(x = op, y = lfc, fill = engine)) +
    geom_col(position = position_dodge(width = 0.7), width = 0.62) +
    geom_errorbar(aes(ymin = lo, ymax = hi), position = position_dodge(width = 0.7),
                  width = 0.3, linewidth = 0.3) +
    geom_hline(yintercept = 0, colour = "#888") +
    coord_flip() +
    scale_y_continuous(breaks = brk, labels = lbl, limits = c(-lim, lim)) +
    scale_fill_manual(values = c("Armadillo-Rock" = "#4285F4", "Armadillo-Rserve" = "#0097A7")) +
    labs(title = tools::toTitleCase(loc), x = NULL, y = NULL, fill = NULL) +
    theme_minimal(base_size = 13, base_family = "IBM Plex Mono") +
    theme(legend.position = "none", plot.margin = margin(10, 14, 5, 8),
          plot.title = element_text(size = 12),
          axis.text.y = element_text(hjust = 0),
          panel.grid.minor = element_blank())
  ggsave(file, p, width = 6.6, height = 4.2, dpi = 300, device = ragg::agg_png)
  cat(sprintf("Wrote %s\n", file))
}
OUT_DIR <- dirname(OUT_CSV)
mk("localhost", file.path(OUT_DIR, "session_local.png"))
mk("remote",    file.path(OUT_DIR, "session_remote.png"))
