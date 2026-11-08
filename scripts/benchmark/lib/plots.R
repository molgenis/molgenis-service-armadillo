# ==============================================================================
# All benchmark figures, rendered to results/. Reads the two measurement CSVs
# (speed_ds_base.csv, speed_dsi.csv); footprint numbers are one-off
# measurements recorded inline (re-measure with resources.sh).
#
#   Rscript plots.R
#
# Produces:
#   total_{local,remote}.png    -- speed vs Opal, per function family
#   session_{local,remote}.png  -- speed vs Opal, per session op
#   res_memory.png              -- resting memory per stack
#   res_storage.png             -- on-disk size of the same 10k-row table
# ==============================================================================

source("helpers.R")            # config.R (RESULTS_DIR) + PRIMITIVES / FAM_ORDER
suppressMessages(library(ggplot2))

OUT      <- RESULTS_DIR
FUN_CSV  <- Sys.getenv("SPEED_DS_BASE_CSV", file.path(RESULTS_DIR, "speed_ds_base.csv"))
SESS_CSV <- Sys.getenv("SPEED_DSI_CSV",  file.path(RESULTS_DIR, "speed_dsi.csv"))

# --- shared diverging log2 "x vs Opal" speed-plot helpers -------------------
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
  present <- unique(as.character(agg$location))
  for (loc in intersect(c("localhost", "remote"), present)) {
    p <- speed_plot(agg[agg$location == loc, ], xvar, limits, tools::toTitleCase(loc))
    f <- file.path(out_dir, sprintf("%s_%s.png", prefix, if (loc == "localhost") "local" else "remote"))
    ggsave(f, p, width = 6.6, height = 4.2, dpi = 300, device = ragg::agg_png)
    cat(sprintf("Wrote %s\n", f))
  }
}

# Everything below depends on complete measurement CSVs. If any figure fails it
# almost always means the benchmark run itself didn't finish (missing or partial
# results), so wrap the whole stage and report that once instead of surfacing an
# opaque low-level error.
tryCatch({

# =============================================================================
# 1. Total speed by function family (speed_ds_base.csv + folded session ops)
# =============================================================================
FAM <- setNames(PRIMITIVES$family, PRIMITIVES$fn)   # fn -> family

d <- read.csv(FUN_CSV, stringsAsFactors = FALSE)
med <- aggregate(roundtrip_ms ~ backend + fn, data = d, FUN = median, na.rm = TRUE, na.action = na.pass)
med$location <- speed_location(med$backend)
med$engine   <- speed_engine(med$backend)

opal <- med[med$engine == "Opal", c("fn", "location", "roundtrip_ms")]
names(opal)[3] <- "opal_rt"
m <- merge(med[med$engine != "Opal", ], opal, by = c("fn", "location"))
m$lfc    <- log2(m$opal_rt / m$roundtrip_ms)   # >0 Armadillo faster, <0 slower; parity (Opal) = 0
m$family <- FAM[m$fn]
m <- m[is.finite(m$lfc) & !is.na(m$family), c("family", "fn", "location", "engine", "lfc")]
mfn <- m   # function-level (keeps fn) for the per-function plots below

# The session ops live in a SEPARATE dataset (speed_dsi.csv, keyed by `op`).
# Fold them into the DSI family so the summary reflects all 9 DSI functions
# (rmDS + the 8 session ops) -- this is what gives the DSI bar its honest spread.
dm <- NULL
if (file.exists(SESS_CSV)) {
  ds <- read.csv(SESS_CSV, stringsAsFactors = FALSE)
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
allf <- rbind(m[, c("family", "location", "engine", "lfc")], dm)

agg <- speed_agg(allf, "family")               # median + IQR of lfc per family x location x engine
agg$family   <- factor(agg$family, levels = rev(FAM_ORDER))
agg$location <- factor(agg$location, levels = c("local", "remote"), labels = c("localhost", "remote"))

# Data is almost all faster, so the axis runs "Opal" (parity) -> right, capping the
# slower side at -1 (2x slower). Shared across both location figures so they compare.
poslim <- max(agg$hi, na.rm = TRUE) * 1.06
speed_save(agg, "family", c(-1, poslim), OUT, "total")

# =============================================================================
# 2. Session / I/O ops by operation (speed_dsi.csv)
# =============================================================================
if (file.exists(SESS_CSV)) {
  d2 <- read.csv(SESS_CSV, stringsAsFactors = FALSE)
  d2 <- d2[!is.na(d2$roundtrip_ms) & d2$roundtrip_ms > 0, ]
  d2$location <- speed_location(d2$backend)
  d2$engine   <- speed_engine(d2$backend)

  opal2 <- aggregate(roundtrip_ms ~ op + location, d2[d2$engine == "Opal", ], median)
  names(opal2)[3] <- "opal_rt"
  a2 <- merge(d2[d2$engine != "Opal", ], opal2, by = c("op", "location"))
  a2$lfc <- log2(a2$opal_rt / a2$roundtrip_ms)      # >0 Armadillo faster, <0 slower

  agg2 <- speed_agg(a2, "op")                        # median + IQR of lfc per op x location x engine
  ord  <- aggregate(lfc ~ op, agg2, median)          # order worst -> best
  agg2$op       <- factor(agg2$op, levels = ord$op[order(ord$lfc)])
  agg2$location <- factor(agg2$location, levels = c("local", "remote"), labels = c("localhost", "remote"))

  # Session ops go both ways (login much slower, assign faster), so keep the axis
  # symmetric: 0 (Opal parity) dead-centre, shared across both location figures.
  lim <- max(abs(c(agg2$lo, agg2$hi)), na.rm = TRUE) * 1.06
  speed_save(agg2, "op", c(-lim, lim), OUT, "session")
}

# =============================================================================
# 3. Per-function breakdown within each family (like the DSI/session per-op plot)
#    -> results/fn_<family>_{local,remote}.png  (DSI is covered by the session plot)
# =============================================================================
for (fam in setdiff(FAM_ORDER, "DSI")) {
  sub <- mfn[mfn$family == fam, ]
  if (!nrow(sub)) next
  aggf <- speed_agg(sub, "fn")
  ord  <- aggregate(lfc ~ fn, aggf, median)          # order worst -> best
  aggf$fn       <- factor(aggf$fn, levels = ord$fn[order(ord$lfc)])
  aggf$location <- factor(aggf$location, levels = c("local", "remote"), labels = c("localhost", "remote"))
  lim  <- max(abs(c(aggf$lo, aggf$hi)), na.rm = TRUE) * 1.06
  slug <- gsub("[^a-z0-9]+", "_", tolower(fam))
  speed_save(aggf, "fn", c(-lim, lim), OUT, paste0("fn_", slug))
}

# =============================================================================
# 4. Footprint -- resting memory + data on disk (one-off measurements)
# =============================================================================
# Resting memory (GiB), idle. Containers (Opal server+Mongo+Rock, Armadillo
# Rock/Rserve engines) via `docker stats` RSS. The Armadillo SERVER is a host JVM
# (not a container), measured with macOS `vmmap <pid> | grep 'physical footprint'`
# -- NOT the actuator jvm.memory.used, which omits native/off-heap. Rock and
# Rserve are alternative Armadillo engines, shown as separate stacks.
APP <- "#4285F4"; DB <- "#6A4C93"; ENG <- "#E6B96A"
mem <- data.frame(
  stack  = c("Opal", "Opal", "Opal",
             "Armadillo (Rock)", "Armadillo (Rock)",
             "Armadillo (Rserve)", "Armadillo (Rserve)"),
  aspect = c("App server", "MongoDB", "R",
             "App server", "R",
             "App server", "R"),
  gib    = c(1.576, 0.242, 0.818, 0.227, 0.479, 0.227, 0.165)
)
mem$stack  <- factor(mem$stack, levels = c("Opal", "Armadillo (Rock)", "Armadillo (Rserve)"))
mem$aspect <- factor(mem$aspect, levels = c("App server", "MongoDB", "R"))
tot <- tapply(mem$gib, mem$stack, sum)
p1 <- ggplot(mem, aes(x = stack, y = gib, fill = aspect)) +
  geom_col(width = 0.62, position = position_stack(reverse = TRUE)) +
  geom_text(data = data.frame(stack = names(tot), gib = as.numeric(tot)),
            aes(x = stack, y = gib, label = sprintf("%.1f GiB", gib)),
            vjust = -0.4, inherit.aes = FALSE, size = 4.2, fontface = "bold") +
  scale_fill_manual(values = c("App server" = APP, "MongoDB" = DB, "R" = ENG)) +
  scale_x_discrete(labels = function(x) sub(" \\(", "\n(", x)) +
  guides(fill = guide_legend(reverse = TRUE)) +
  labs(title = "Resting memory footprint", x = NULL, y = "GiB resident", fill = NULL) +
  scale_y_continuous(limits = c(0, 3), breaks = 0:3, expand = expansion(mult = c(0, 0.03))) +
  theme_minimal(base_size = 13, base_family = "IBM Plex Mono") +
  theme(legend.position = "right", plot.title = element_text(size = 12),
        plot.margin = margin(10, 15, 5, 10))
ggsave(file.path(OUT, "res_memory.png"), p1, width = 6.6, height = 4.2, dpi = 300, device = ragg::agg_png)
cat("Wrote", file.path(OUT, "res_memory.png"), "\n")

# Data on disk: CNSIM (10k rows x 29 cols), Parquet vs Opal Mongo value_set.
sto <- data.frame(
  backend = c("Opal (Mongo)", "Armadillo (Parquet)"),
  kb      = c(2140, 196)
)
sto$mb <- sto$kb / 1024
sto$backend <- factor(sto$backend, levels = c("Opal (Mongo)", "Armadillo (Parquet)"))
p2 <- ggplot(sto, aes(x = backend, y = mb)) +
  geom_col(width = 0.55, fill = "#0097A7") +
  geom_text(aes(label = sprintf("%.2f MB", mb)), vjust = -0.4, size = 4.2, fontface = "bold", colour = "#444") +
  scale_x_discrete(labels = function(x) sub(" \\(", "\n(", x)) +
  labs(title = "Data on disk — CNSIM (10,000 rows x 29 cols)", x = NULL, y = "MB on disk", fill = NULL) +
  scale_y_continuous(limits = c(0, 3), breaks = 0:3, expand = expansion(mult = c(0, 0.03))) +
  theme_minimal(base_size = 13, base_family = "IBM Plex Mono") +
  theme(legend.position = "none", plot.title = element_text(size = 12),
        plot.margin = margin(10, 15, 5, 10))
ggsave(file.path(OUT, "res_storage.png"), p2, width = 6.6, height = 4.2, dpi = 300, device = ragg::agg_png)
cat("Wrote", file.path(OUT, "res_storage.png"), "\n")

}, error = function(e) {
  message("Plotting failed. Check the benchmark run completed correctly.")
  message("  reason: ", conditionMessage(e))
})
