# ==============================================================================
# Chart the benchmark results, SPLIT into localhost vs remote. For each op, show
# how many times faster/slower each Armadillo profile is than Opal *in the same
# environment* (mean ops/sec across reps). One panel per environment; Opal is the
# 1x baseline (0 line), bars right = faster than that env's Opal, left = slower.
#
#   Rscript plot.R
#
# Requires ggplot2 (in addition to the benchmark prerequisites).
# Output: results/comparison.png
# ==============================================================================

source("config.R")
suppressMessages(library(ggplot2))

stopifnot(file.exists(OUT_CSV))
d <- read.csv(OUT_CSV, stringsAsFactors = FALSE)

# Mean & SD of rate per (backend, op); helpers to pull one backend's op -> vector.
m    <- aggregate(rate ~ backend + op, data = d, FUN = mean)
sdev <- aggregate(rate ~ backend + op, data = d, FUN = sd)
rate_of <- function(be) setNames(m$rate[m$backend == be], m$op[m$backend == be])
sd_of   <- function(be) setNames(sdev$rate[sdev$backend == be], sdev$op[sdev$backend == be])

# Compare each environment's Armadillo profiles to THAT environment's Opal.
locs <- intersect(c("local", "remote"), unique(vapply(BACKENDS, backend_location, character(1))))
rows <- list()
for (loc in locs) {
  base    <- rate_of(paste0("opal_", loc))
  base_sd <- sd_of(paste0("opal_", loc))
  if (!length(base)) next
  arma_bes <- BACKENDS[backend_kind(BACKENDS) == "armadillo" &
                       vapply(BACKENDS, backend_location, character(1)) == loc]
  for (be in arma_bes) {
    r   <- rate_of(be)
    rsd <- sd_of(be)
    ops <- intersect(names(base), names(r))
    if (!length(ops)) next
    fold    <- ifelse(r[ops] >= base[ops], r[ops] / base[ops], -(base[ops] / r[ops]))
    # Delta method: the ratio's relative SD is the quadrature sum of each rate's.
    rel_sd  <- sqrt((rsd[ops] / r[ops])^2 + (base_sd[ops] / base[ops])^2)
    fold_sd <- abs(fold) * rel_sd
    profile <- if (grepl("_rserve$", be)) "rserve" else "default"
    rows[[be]] <- data.frame(op = ops, location = loc, profile = profile,
                             fold = as.numeric(fold), fold_sd = as.numeric(fold_sd),
                             row.names = NULL)
  }
}
cmp <- do.call(rbind, rows)
stopifnot(!is.null(cmp), nrow(cmp) > 0)

# Order ops by overall mean advantage; consistent across panels.
op_order     <- names(sort(tapply(cmp$fold, cmp$op, mean)))
cmp$op       <- factor(cmp$op, levels = op_order)
cmp$location <- factor(cmp$location, levels = locs)
cmp$profile  <- factor(cmp$profile, levels = intersect(c("default", "rserve"), unique(cmp$profile)))

# Coarse analytical family per op (from the 16-way category carried in the CSV).
cat_of <- setNames(d$category, d$op)[!duplicated(d$op)]
GROUP  <- c(descriptive = "Summary statistics", correlation = "Summary statistics",
            tabulation  = "Summary statistics", metadata = "Metadata / introspection",
            coercion = "Transform & recode", transform = "Transform & recode",
            recode = "Transform & recode", vector = "Transform & recode",
            dataframe = "Data-frame manipulation", reshape = "Data-frame manipulation",
            glm = "Modelling", `mixed-model` = "Modelling",
            io = "Session & infrastructure", dsi = "Session & infrastructure",
            objects = "Session & infrastructure", session = "Session & infrastructure")
GROUP_ORDER <- c("Summary statistics", "Metadata / introspection", "Transform & recode",
                 "Data-frame manipulation", "Modelling", "Session & infrastructure")
cmp$group <- factor(GROUP[cat_of[as.character(cmp$op)]], levels = GROUP_ORDER)

lim  <- max(abs(cmp$fold) + ifelse(is.na(cmp$fold_sd), 0, cmp$fold_sd)) * 1.05
PLOT <- file.path(dirname(OUT_CSV), "comparison.png")
env_labels <- c(local = "localhost", remote = "remote")
p <- ggplot(cmp, aes(x = op, y = fold, fill = profile)) +
  geom_col(width = 0.7, position = position_dodge(width = 0.8)) +
  geom_errorbar(aes(ymin = fold - fold_sd, ymax = fold + fold_sd),
                width = 0.3, linewidth = 0.3,
                position = position_dodge(width = 0.8), na.rm = TRUE) +
  geom_hline(yintercept = 0, linewidth = 0.4) +
  coord_flip() +
  facet_grid(group ~ location, scales = "free_y", space = "free_y",
             labeller = labeller(location = env_labels, group = label_wrap_gen(14))) +
  scale_y_continuous(limits = c(-lim, lim), labels = function(x) sprintf("%g×", abs(x))) +
  scale_fill_manual(values = c(default = "#00BFC4", rserve = "#C77CFF")) +
  labs(title = "Armadillo throughput relative to Opal, per op",
       subtitle = sprintf("× faster / slower than Opal in the SAME environment (mean ops/sec ± propagated SD, %d rep(s)); 0 = parity",
                          length(unique(d$rep))),
       x = NULL,
       y = "← slower than Opal        faster than Opal →",
       fill = "Armadillo profile") +
  theme_minimal(base_size = 12)

ggsave(PLOT, p, width = 11, height = 10, dpi = 150)
cat(sprintf("Wrote %s\n", PLOT))

# --- Domain summary: mean fold per family (± SD across that family's ops) ------
sm    <- aggregate(fold ~ group + location + profile, data = cmp, FUN = mean)
sm$sd <- aggregate(fold ~ group + location + profile, data = cmp, FUN = sd)$fold
sm$group <- factor(sm$group, levels = rev(GROUP_ORDER))
lim2 <- max(abs(sm$fold) + ifelse(is.na(sm$sd), 0, sm$sd)) * 1.05
SUM  <- file.path(dirname(OUT_CSV), "comparison_summary.png")
ps <- ggplot(sm, aes(x = group, y = fold, fill = profile)) +
  geom_col(width = 0.7, position = position_dodge(width = 0.8)) +
  geom_errorbar(aes(ymin = fold - sd, ymax = fold + sd), width = 0.3, linewidth = 0.3,
                position = position_dodge(width = 0.8), na.rm = TRUE) +
  geom_hline(yintercept = 0, linewidth = 0.4) +
  coord_flip() +
  facet_wrap(~ location, labeller = labeller(location = env_labels)) +
  scale_y_continuous(limits = c(-lim2, lim2), labels = function(x) sprintf("%g×", abs(x))) +
  scale_fill_manual(values = c(default = "#00BFC4", rserve = "#C77CFF")) +
  labs(title = "Armadillo throughput relative to Opal, by function family",
       subtitle = "Mean × faster / slower than Opal across each family's ops (± SD across ops); 0 = parity",
       x = NULL, y = "← slower than Opal        faster than Opal →",
       fill = "Armadillo profile") +
  theme_minimal(base_size = 12)

ggsave(SUM, ps, width = 10, height = 4.5, dpi = 150)
cat(sprintf("Wrote %s\n", SUM))

# --- One graph per family: its ops + an AVERAGE bar (family mean) ------------
slug <- function(x) gsub("_+", "_", gsub("[^a-z0-9]+", "_", tolower(x)))
cols <- c("op", "location", "profile", "fold", "fold_sd")
for (g in GROUP_ORDER) {
  dg <- cmp[cmp$group == g, ]
  if (!nrow(dg)) next
  avg         <- aggregate(fold ~ location + profile, data = dg, FUN = mean)
  avg$fold_sd <- aggregate(fold ~ location + profile, data = dg, FUN = sd)$fold
  avg$op      <- "AVERAGE"
  dg2 <- rbind(dg[, cols], avg[, cols])
  dg2$is_avg <- dg2$op == "AVERAGE"
  ord <- names(sort(tapply(dg$fold, as.character(dg$op), mean)))
  dg2$op <- factor(dg2$op, levels = c(ord, "AVERAGE"))
  limg <- max(abs(dg2$fold) + ifelse(is.na(dg2$fold_sd), 0, dg2$fold_sd)) * 1.05
  pg <- ggplot(dg2, aes(x = op, y = fold, fill = profile)) +
    geom_col(aes(color = is_avg), width = 0.7, linewidth = 0.5,
             position = position_dodge(width = 0.8)) +
    geom_errorbar(aes(ymin = fold - fold_sd, ymax = fold + fold_sd), width = 0.3,
                  linewidth = 0.3, position = position_dodge(width = 0.8), na.rm = TRUE) +
    geom_hline(yintercept = 0, linewidth = 0.4) +
    coord_flip() +
    facet_wrap(~ location, labeller = labeller(location = env_labels)) +
    scale_y_continuous(limits = c(-limg, limg), labels = function(x) sprintf("%g×", abs(x))) +
    scale_fill_manual(values = c(default = "#00BFC4", rserve = "#C77CFF")) +
    scale_color_manual(values = c(`FALSE` = NA, `TRUE` = "black"), guide = "none") +
    labs(title = sprintf("Armadillo throughput relative to Opal — %s", g),
         subtitle = "× faster / slower than Opal (ops: ± propagated SD; AVERAGE: family mean ± SD across ops); 0 = parity",
         x = NULL, y = "← slower than Opal        faster than Opal →",
         fill = "Armadillo profile") +
    theme_minimal(base_size = 12)
  fn <- file.path(dirname(OUT_CSV), sprintf("family_%s.png", slug(g)))
  ggsave(fn, pg, width = 10, height = max(2.8, 0.42 * length(levels(dg2$op)) + 1.4), dpi = 150)
  cat(sprintf("Wrote %s\n", fn))
}
