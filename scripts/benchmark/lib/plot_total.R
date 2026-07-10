# ==============================================================================
# Total performance, by category: how many times faster each Armadillo engine is
# than Opal (round-trip), averaged per function family, localhost vs remote.
# Opal is the parity reference (dashed line at 1). Reads speed_true.csv.
# Output: results/total.png
#   Rscript speed_true.R && Rscript plot_total.R
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
  rmDS = "Session & I/O"
)
FAM_ORDER <- c("Summary statistics", "Metadata", "Transform & recode", "Data-frame", "Modelling", "Session & I/O")

med <- aggregate(roundtrip_ms ~ backend + fn, data = d, FUN = median, na.rm = TRUE, na.action = na.pass)
med$location <- ifelse(grepl("remote", med$backend), "remote", "local")
med$engine <- ifelse(grepl("^opal", med$backend), "Opal",
              ifelse(grepl("rserve", med$backend), "Armadillo-Rserve", "Armadillo-Rock"))

opal <- med[med$engine == "Opal", c("fn", "location", "roundtrip_ms")]
names(opal)[3] <- "opal_rt"
m <- merge(med[med$engine != "Opal", ], opal, by = c("fn", "location"))
m$speedup <- m$opal_rt / m$roundtrip_ms
m$family  <- FAM[m$fn]
m <- m[is.finite(m$speedup) & !is.na(m$family), ]

q <- function(x, p) as.numeric(quantile(x, p, na.rm = TRUE, names = FALSE))
agg    <- aggregate(speedup ~ family + location + engine, m, median)
agg$lo <- aggregate(speedup ~ family + location + engine, m, q, 0.25)$speedup
agg$hi <- aggregate(speedup ~ family + location + engine, m, q, 0.75)$speedup
agg$family   <- factor(agg$family, levels = rev(FAM_ORDER))
agg$location <- factor(agg$location, levels = c("remote", "local"), labels = c("remote", "localhost"))

PLOT <- file.path(dirname(OUT_CSV), "total.png")
p <- ggplot(agg, aes(x = family, y = speedup, fill = engine)) +
  geom_col(position = position_dodge(width = 0.7), width = 0.62) +
  geom_errorbar(aes(ymin = lo, ymax = hi), position = position_dodge(width = 0.7),
                width = 0.3, linewidth = 0.3) +
  geom_hline(yintercept = 1, linetype = "dashed", colour = "#888") +
  facet_wrap(~ location) +
  coord_flip() +
  scale_fill_manual(values = c("Armadillo-Rock" = "#4285F4", "Armadillo-Rserve" = "#0097A7")) +
  labs(title = "Round-trip vs Opal, by category",
       subtitle = "Median × faster than Opal per family; IQR whiskers across the family's ops",
       x = NULL, y = "× faster than Opal", fill = NULL) +
  theme_minimal(base_size = 12) +
  theme(legend.position = "top")

ggsave(PLOT, p, width = 10, height = 4.2, dpi = 150)
cat(sprintf("Wrote %s\n", PLOT))
