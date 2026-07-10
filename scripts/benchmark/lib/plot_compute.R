# ==============================================================================
# Speed decomposition plot: for each backend x single-command primitive, the
# server compute time and the full tight-poll round trip, side by side.
#   compute_ms    true server execution (endDate - startDate)   [speed_true.csv]
#   roundtrip_ms  low-level submit -> tight 2 ms poll -> fetch   [speed_true.csv]
# transport = roundtrip - compute (serialisation + protocol + network). The DSI
# client poll is held at 2 ms for EVERY test, so poll wait is not a component here
# (the 50 ms default would distort fast local calls -- see the methods note).
#
#   Rscript speed_true.R       # produces speed_true.csv first
#   Rscript plot_compute.R
#
# Requires ggplot2. Output: results/compute.png  (+ a console summary table)
# ==============================================================================

source("config.R")
suppressMessages(library(ggplot2))

TRUE_CSV <- Sys.getenv("SPEED_TRUE_CSV", file.path(dirname(OUT_CSV), "speed_true.csv"))
stopifnot(file.exists(TRUE_CSV))
dt <- read.csv(TRUE_CSV, stringsAsFactors = FALSE)
stopifnot(nrow(dt) > 0)

MEAS <- c("compute_ms", "roundtrip_ms")

# Median per (backend, fn) with 25/75th percentiles for the whiskers -- robust to
# the occasional GC/network spike and to the millisecond server-timestamp quantum.
q25 <- function(x) as.numeric(quantile(x, 0.25, na.rm = TRUE, names = FALSE))
q75 <- function(x) as.numeric(quantile(x, 0.75, na.rm = TRUE, names = FALSE))
med <- aggregate(cbind(compute_ms, roundtrip_ms) ~ backend + fn, data = dt, FUN = median, na.rm = TRUE, na.action = na.pass)
lo  <- aggregate(cbind(compute_ms, roundtrip_ms) ~ backend + fn, data = dt, FUN = q25, na.action = na.pass)
hi  <- aggregate(cbind(compute_ms, roundtrip_ms) ~ backend + fn, data = dt, FUN = q75, na.action = na.pass)
names(lo)[match(MEAS, names(lo))] <- paste0(MEAS, "_lo")
names(hi)[match(MEAS, names(hi))] <- paste0(MEAS, "_hi")
med <- Reduce(function(a, b) merge(a, b, by = c("backend", "fn"), all = TRUE), list(med, lo, hi))

# --- Console summary --------------------------------------------------------
s <- med
s$transport_ms <- s$roundtrip_ms - s$compute_ms
s <- s[order(s$fn, s$backend), ]
reps <- length(unique(dt$rep))
cat(sprintf("Compute vs round-trip (median over %d rep(s))\n\n", reps))
print(within(s, {
  compute_ms   <- round(compute_ms, 1); roundtrip_ms <- round(roundtrip_ms, 1)
  transport_ms <- round(transport_ms, 1)
}), row.names = FALSE)

# --- Plot -------------------------------------------------------------------
long <- reshape(med, varying = list(MEAS, paste0(MEAS, "_lo"), paste0(MEAS, "_hi")),
                v.names = c("ms", "ms_lo", "ms_hi"), timevar = "measure",
                times = MEAS, direction = "long")
long$measure <- factor(long$measure, levels = MEAS,
                       labels = c("server compute", "round trip"))

PLOT <- file.path(dirname(OUT_CSV), "compute.png")
p <- ggplot(long, aes(x = backend, y = ms, fill = measure)) +
  geom_col(width = 0.7, position = position_dodge(width = 0.8)) +
  geom_errorbar(aes(ymin = ms_lo, ymax = ms_hi),
                width = 0.25, linewidth = 0.3,
                position = position_dodge(width = 0.8), na.rm = TRUE) +
  facet_wrap(~ fn, scales = "free_y") +
  scale_y_continuous(labels = function(x) sprintf("%g", x)) +
  scale_fill_manual(values = c("server compute" = "#1A9850", "round trip" = "#4285F4")) +
  labs(title = "Server compute vs round-trip time",
       subtitle = "Single-command primitives (median bars, IQR whiskers); the gap above green is transport (serialise + protocol + network)",
       x = NULL, y = "milliseconds (median)", fill = NULL) +
  theme_minimal(base_size = 12) +
  theme(legend.position = "top")

ggsave(PLOT, p, width = 9, height = 5, dpi = 150)
cat(sprintf("\nWrote %s\n", PLOT))
