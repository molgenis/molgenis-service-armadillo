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
source("speed_plot_lib.R")

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
med$location <- speed_location(med$backend)
med$engine   <- speed_engine(med$backend)

opal <- med[med$engine == "Opal", c("fn", "location", "roundtrip_ms")]
names(opal)[3] <- "opal_rt"
m <- merge(med[med$engine != "Opal", ], opal, by = c("fn", "location"))
m$lfc    <- log2(m$opal_rt / m$roundtrip_ms)   # >0 Armadillo faster, <0 slower; parity (Opal) = 0
m$family <- FAM[m$fn]
m <- m[is.finite(m$lfc) & !is.na(m$family), c("family", "location", "engine", "lfc")]

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
  dmed$location <- speed_location(dmed$backend)
  dmed$engine   <- speed_engine(dmed$backend)
  dopal <- dmed[dmed$engine == "Opal", c("op", "location", "roundtrip_ms")]
  names(dopal)[3] <- "opal_rt"
  dm <- merge(dmed[dmed$engine != "Opal", ], dopal, by = c("op", "location"))
  dm$lfc    <- log2(dm$opal_rt / dm$roundtrip_ms)
  dm$family <- "DSI"
  dm <- dm[is.finite(dm$lfc), c("family", "location", "engine", "lfc")]
}
allf <- rbind(m, dm)

agg <- speed_agg(allf, "family")               # median + IQR of lfc per family x location x engine
agg$family   <- factor(agg$family, levels = rev(FAM_ORDER))
agg$location <- factor(agg$location, levels = c("local", "remote"), labels = c("localhost", "remote"))

# Data is almost all faster, so the axis runs "Opal" (parity) -> right, capping the
# slower side at -1 (2x slower). Shared across both location figures so they compare.
poslim <- max(agg$hi, na.rm = TRUE) * 1.06
speed_save(agg, "family", c(-1, poslim), dirname(OUT_CSV), "total")
