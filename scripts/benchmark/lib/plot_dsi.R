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
agg$location <- factor(agg$location, levels = c("remote", "local"), labels = c("remote", "localhost"))

brk <- -4:3
lbl <- c("16x", "8x", "4x", "2x", "Opal", "2x", "4x", "8x")   # |fold|; left slower, right faster
PLOT <- file.path(dirname(OUT_CSV), "session_dsi.png")
p <- ggplot(agg, aes(x = op, y = lfc, fill = engine)) +
  geom_col(position = position_dodge(width = 0.7), width = 0.62) +
  geom_errorbar(aes(ymin = lo, ymax = hi), position = position_dodge(width = 0.7),
                width = 0.3, linewidth = 0.3) +
  geom_hline(yintercept = 0, colour = "#888") +
  facet_wrap(~ location) +
  coord_flip() +
  scale_y_continuous(breaks = brk, labels = lbl) +
  scale_fill_manual(values = c("Armadillo-Rock" = "#4285F4", "Armadillo-Rserve" = "#0097A7")) +
  labs(title = "Session & I/O vs Opal, by operation",
       subtitle = "Left of centre = Armadillo slower  ·  right = faster (fold vs Opal); IQR whiskers",
       x = NULL, y = NULL, fill = NULL) +
  theme_minimal(base_size = 12) +
  theme(legend.position = "top")

ggsave(PLOT, p, width = 10, height = 4.6, dpi = 150)
cat(sprintf("Wrote %s\n", PLOT))
