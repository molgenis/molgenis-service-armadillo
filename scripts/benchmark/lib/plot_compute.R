# ==============================================================================
# Consolidated speed plot: for each backend x single-command primitive, three
# times side by side, joined from the two speed suites:
#   compute_ms   true server execution (endDate - startDate)   [speed_true.csv]
#   roundtrip_ms low-level submit -> poll(2 ms) -> fetch        [speed_true.csv]
#   client_ms    high-level datashield.* call (default poll)    [speed_client.csv]
# The gap between compute_ms and client_ms is the client-side polling penalty.
#
#   Rscript speed_true.R && Rscript speed_client.R   # produce the two CSVs first
#   Rscript plot_compute.R
#
# Requires ggplot2. Output: results/compute.png  (+ a console summary table)
# ==============================================================================

source("config.R")
suppressMessages(library(ggplot2))

TRUE_CSV   <- Sys.getenv("SPEED_TRUE_CSV",   file.path(dirname(OUT_CSV), "speed_true.csv"))
CLIENT_CSV <- Sys.getenv("SPEED_CLIENT_CSV", file.path(dirname(OUT_CSV), "speed_client.csv"))
stopifnot(file.exists(TRUE_CSV), file.exists(CLIENT_CSV))
dt <- read.csv(TRUE_CSV,   stringsAsFactors = FALSE)
dc <- read.csv(CLIENT_CSV, stringsAsFactors = FALSE)
stopifnot(nrow(dt) > 0, nrow(dc) > 0)

MEAS <- c("compute_ms", "roundtrip_ms", "client_ms")

# Median per (backend, fn) -- robust to the occasional GC/network spike and to
# the millisecond quantisation of the server timestamps. Join true + client.
med_true <- aggregate(cbind(compute_ms, roundtrip_ms) ~ backend + fn, data = dt,
                      FUN = median, na.rm = TRUE, na.action = na.pass)
med_clnt <- aggregate(client_ms ~ backend + fn, data = dc,
                      FUN = median, na.rm = TRUE, na.action = na.pass)
med <- merge(med_true, med_clnt, by = c("backend", "fn"), all = TRUE)

# --- Console summary --------------------------------------------------------
# penalty = how many times longer the client-observed time is than true compute;
# pct_waiting = share of the client-observed time that is NOT server compute.
s <- med
s$penalty     <- s$client_ms / s$compute_ms
s$pct_waiting <- 100 * (s$client_ms - s$compute_ms) / s$client_ms
s <- s[order(s$fn, s$backend), ]
reps <- length(unique(dt$rep))
cat(sprintf("True compute vs client-observed (median over %d rep(s))\n\n", reps))
print(within(s, {
  compute_ms   <- round(compute_ms, 1); roundtrip_ms <- round(roundtrip_ms, 1)
  client_ms    <- round(client_ms, 1);  penalty      <- round(penalty, 1)
  pct_waiting  <- round(pct_waiting, 1)
}), row.names = FALSE)

# --- Plot -------------------------------------------------------------------
# Long format, measures ordered fastest -> slowest so the legend reads in order.
long <- reshape(med, varying = MEAS, v.names = "ms", timevar = "measure",
                times = MEAS, direction = "long")
long$measure <- factor(long$measure, levels = MEAS,
                       labels = c("server compute", "round trip (tight poll)", "client (default poll)"))

PLOT <- file.path(dirname(OUT_CSV), "compute.png")
p <- ggplot(long, aes(x = backend, y = ms, fill = measure)) +
  geom_col(width = 0.7, position = position_dodge(width = 0.8)) +
  facet_wrap(~ fn, scales = "free_y") +
  scale_y_continuous(labels = function(x) sprintf("%g", x)) +
  scale_fill_manual(values = c("server compute"          = "#1A9850",
                               "round trip (tight poll)"  = "#4285F4",
                               "client (default poll)"    = "#D73027")) +
  labs(title = "True server compute vs client-observed time",
       subtitle = "Single-command primitives; the green-to-red gap is the DSI poll-sleep penalty",
       x = NULL, y = "milliseconds (median)", fill = NULL) +
  theme_minimal(base_size = 12) +
  theme(legend.position = "top")

ggsave(PLOT, p, width = 9, height = 5, dpi = 150)
cat(sprintf("\nWrote %s\n", PLOT))
