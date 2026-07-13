# ==============================================================================
# Total performance, by category: how many times faster each Armadillo engine is
# than Opal (round-trip), averaged per function family, localhost vs remote.
# Diverging log2 fold-change, "Opal" (parity) at centre. Reads speed_true.csv
# (analytical fns + rmDS) AND speed_dsi.csv (the 8 session ops), which are folded
# into the DSI family so it reflects all 9 DSI functions.
# Output: results/total_local.png + results/total_remote.png (one per location,
#   sized to match the footprint charts so they line up on the slide).
#   Rscript speed_true.R && Rscript speed_dsi.R && Rscript plot_total.R
# ==============================================================================

source("config.R")
suppressMessages(library(ggplot2))

TRUE_CSV <- Sys.getenv("SPEED_TRUE_CSV", file.path(dirname(OUT_CSV), "speed_true.csv"))
stopifnot(file.exists(TRUE_CSV))
d <- read.csv(TRUE_CSV, stringsAsFactors = FALSE)

# fn -> family (the six analytical families; arithmetic assigns are dropped)
FAM <- c(
  meanDS = "Summary statistics", varDS = "Summary statistics", quantileMeanDS = "Summary statistics",
  corDS = "Summary statistics", tableDS = "Summary statistics",
  classDS = "Metadata", dimDS = "Metadata", colnamesDS = "Metadata", lengthDS = "Metadata",
  levelsDS = "Metadata", numNaDS = "Metadata", lsDS = "Metadata", isValidDS = "Metadata",
  asFactorDS1 = "Transform & recode", asFactorDS2 = "Transform & recode", asIntegerDS = "Transform & recode",
  asCharacterDS = "Transform & recode", asDataMatrixDS = "Transform & recode", BooleDS = "Transform & recode",
  recodeValuesDS = "Transform & recode", recodeLevelsDS = "Transform & recode",
  changeRefGroupDS = "Transform & recode", repDS = "Transform & recode", replaceNaDS = "Transform & recode",
  dataFrameDS = "Data-frame", dataFrameSubsetDS1 = "Data-frame", dataFrameSubsetDS2 = "Data-frame",
  cbindDS = "Data-frame", mergeDS = "Data-frame", reShapeDS = "Data-frame",
  glmDS1 = "Modelling", glmDS2 = "Modelling", glmSLMADS1 = "Modelling",
  "glmSLMADS.assign" = "Modelling", lmerSLMADS2 = "Modelling",
  rmDS = "DSI"
)
FAM_ORDER <- c("Summary statistics", "Metadata", "Transform & recode", "Data-frame", "Modelling", "DSI")

med <- aggregate(roundtrip_ms ~ backend + fn, data = d, FUN = median, na.rm = TRUE, na.action = na.pass)
med$location <- ifelse(grepl("remote", med$backend), "remote", "local")
med$engine <- ifelse(grepl("^opal", med$backend), "Opal",
              ifelse(grepl("rserve", med$backend), "Armadillo-Rserve", "Armadillo-Rock"))

opal <- med[med$engine == "Opal", c("fn", "location", "roundtrip_ms")]
names(opal)[3] <- "opal_rt"
m <- merge(med[med$engine != "Opal", ], opal, by = c("fn", "location"))
m$speedup <- m$opal_rt / m$roundtrip_ms
m$lfc     <- log2(m$speedup)     # >0 Armadillo faster, <0 slower; parity (Opal) = 0
m$family  <- FAM[m$fn]
m <- m[is.finite(m$lfc) & !is.na(m$family), ]

# The 8 session ops (login, assign.table, workspace_*, ...) live in a SEPARATE
# dataset (speed_dsi.csv, keyed by `op`). Fold them into the DSI family so the
# summary reflects all 9 DSI functions (rmDS + the session ops) as the methods
# table lists — this is what gives the DSI bar its large, honest spread.
DSI_CSV <- Sys.getenv("SPEED_DSI_CSV", file.path(dirname(OUT_CSV), "speed_dsi.csv"))
dm <- NULL
if (file.exists(DSI_CSV)) {
  ds <- read.csv(DSI_CSV, stringsAsFactors = FALSE)
  ds <- ds[!is.na(ds$roundtrip_ms) & ds$roundtrip_ms > 0, ]
  dmed <- aggregate(roundtrip_ms ~ backend + op, data = ds, FUN = median)
  dmed$location <- ifelse(grepl("remote", dmed$backend), "remote", "local")
  dmed$engine   <- ifelse(grepl("^opal", dmed$backend), "Opal",
                   ifelse(grepl("rserve", dmed$backend), "Armadillo-Rserve", "Armadillo-Rock"))
  dopal <- dmed[dmed$engine == "Opal", c("op", "location", "roundtrip_ms")]
  names(dopal)[3] <- "opal_rt"
  dm <- merge(dmed[dmed$engine != "Opal", ], dopal, by = c("op", "location"))
  dm$lfc    <- log2(dm$opal_rt / dm$roundtrip_ms)
  dm$family <- "DSI"
  dm <- dm[is.finite(dm$lfc), c("family", "location", "engine", "lfc")]
}
allf <- rbind(m[, c("family", "location", "engine", "lfc")], dm)

q <- function(x, p) as.numeric(quantile(x, p, na.rm = TRUE, names = FALSE))
agg    <- aggregate(lfc ~ family + location + engine, allf, median)
agg$lo <- aggregate(lfc ~ family + location + engine, allf, q, 0.25)$lfc
agg$hi <- aggregate(lfc ~ family + location + engine, allf, q, 0.75)$lfc
agg$family   <- factor(agg$family, levels = rev(FAM_ORDER))
agg$location <- factor(agg$location, levels = c("local", "remote"), labels = c("localhost", "remote"))

# Symmetric log2 axis with "Opal" at the centre — same diverging design as session_dsi.
poslim <- max(agg$hi, na.rm = TRUE) * 1.06     # data is almost all faster; cap slower side at -1
brk <- -4:4
lbl <- c("16x", "8x", "4x", "2x", "Opal", "2x", "4x", "8x", "16x")

# One self-contained figure per location (localhost/remote), so they can sit
# side-by-side on the slide like the memory/storage pair. Shared axis (lim) keeps
# them comparable; legend lives on the slide, not the plots.
mk <- function(loc, file) {
  sub <- agg[agg$location == loc, ]
  p <- ggplot(sub, aes(x = family, y = lfc, fill = engine)) +
    geom_col(position = position_dodge(width = 0.7), width = 0.62) +
    geom_errorbar(aes(ymin = lo, ymax = hi), position = position_dodge(width = 0.7),
                  width = 0.3, linewidth = 0.3) +
    geom_hline(yintercept = 0, colour = "#888") +
    coord_flip() +
    scale_y_continuous(breaks = brk, labels = lbl, limits = c(-1, poslim)) +
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
mk("localhost", file.path(OUT_DIR, "total_local.png"))
mk("remote",    file.path(OUT_DIR, "total_remote.png"))
