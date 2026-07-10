# ==============================================================================
# Comparison plot from the primitives' round-trip: how many times faster each
# Armadillo engine is than Opal, per single-command primitive, local vs remote.
#   speedup = Opal round-trip / Armadillo round-trip   (> 1 => Armadillo faster)
# Reads speed_true.csv. Output: results/compare.png
#
#   Rscript speed_true.R && Rscript plot_compare.R
# ==============================================================================

source("config.R")
suppressMessages(library(ggplot2))

TRUE_CSV <- Sys.getenv("SPEED_TRUE_CSV", file.path(dirname(OUT_CSV), "speed_true.csv"))
stopifnot(file.exists(TRUE_CSV))
d <- read.csv(TRUE_CSV, stringsAsFactors = FALSE)

med <- aggregate(roundtrip_ms ~ backend + fn, data = d, FUN = median, na.rm = TRUE, na.action = na.pass)
med$location <- ifelse(grepl("remote", med$backend), "remote", "local")
med$engine <- ifelse(grepl("^opal", med$backend), "Opal",
              ifelse(grepl("rserve", med$backend), "Armadillo-Rserve", "Armadillo-Rock"))

opal <- med[med$engine == "Opal", c("fn", "location", "roundtrip_ms")]
names(opal)[3] <- "opal_rt"
arm <- med[med$engine != "Opal", ]
m <- merge(arm, opal, by = c("fn", "location"))
m$speedup <- m$opal_rt / m$roundtrip_ms
m <- m[is.finite(m$speedup), ]
m$location <- factor(m$location, levels = c("remote", "local"),
                     labels = c("remote", "localhost"))

# order primitives by overall mean speedup so the plot reads top-to-bottom
ord <- aggregate(speedup ~ fn, m, mean)
m$fn <- factor(m$fn, levels = ord$fn[order(ord$speedup)])

PLOT <- file.path(dirname(OUT_CSV), "compare.png")
p <- ggplot(m, aes(x = fn, y = speedup, fill = engine)) +
  geom_col(position = position_dodge(width = 0.7), width = 0.65) +
  geom_hline(yintercept = 1, linetype = "dashed", colour = "#888") +
  facet_wrap(~ location) +
  coord_flip() +
  scale_fill_manual(values = c("Armadillo-Rock" = "#4285F4", "Armadillo-Rserve" = "#0097A7")) +
  labs(title = "Round-trip: how many times faster than Opal",
       subtitle = "Opal round-trip ÷ Armadillo, per primitive (median, n = 20); dashed line = parity",
       x = NULL, y = "× faster than Opal", fill = NULL) +
  theme_minimal(base_size = 11) +
  theme(legend.position = "top", axis.text.y = element_text(size = 6),
        panel.grid.major.y = element_blank())

ggsave(PLOT, p, width = 9, height = 6, dpi = 150)
cat(sprintf("Wrote %s\n", PLOT))
