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
source("speed_plot_lib.R")

DSI_CSV <- Sys.getenv("SPEED_DSI_CSV", file.path(dirname(OUT_CSV), "speed_dsi.csv"))
stopifnot(file.exists(DSI_CSV))
d <- read.csv(DSI_CSV, stringsAsFactors = FALSE)
d <- d[!is.na(d$roundtrip_ms) & d$roundtrip_ms > 0, ]
d$location <- speed_location(d$backend)
d$engine   <- speed_engine(d$backend)

# Opal reference: median round-trip per op x location, then log2 fold per arma rep
opal <- aggregate(roundtrip_ms ~ op + location, d[d$engine == "Opal", ], median)
names(opal)[3] <- "opal_rt"
a <- merge(d[d$engine != "Opal", ], opal, by = c("op", "location"))
a$lfc <- log2(a$opal_rt / a$roundtrip_ms)          # >0 Armadillo faster, <0 slower

agg <- speed_agg(a, "op")                          # median + IQR of lfc per op x location x engine
ord <- aggregate(lfc ~ op, agg, median)            # order worst -> best
agg$op       <- factor(agg$op, levels = ord$op[order(ord$lfc)])
agg$location <- factor(agg$location, levels = c("local", "remote"), labels = c("localhost", "remote"))

# Session ops go both ways (login much slower, assign faster), so keep the axis
# symmetric: 0 (Opal parity) dead-centre, shared across both location figures.
lim <- max(abs(c(agg$lo, agg$hi)), na.rm = TRUE) * 1.06
speed_save(agg, "op", c(-lim, lim), dirname(OUT_CSV), "session")
