# ==============================================================================
# Resource footprint of the LOCAL 2-core/8GB backends (measured once, idle, via
# resources.sh + du of the Parquet store + Mongo opal_data collection stats).
# Two figures:
#   res_memory.png  - resting memory per stack (Opal vs Armadillo), by component
#   res_storage.png - on-disk size of the same 10k-row tables (Parquet vs Mongo)
# Values are one-off measurements recorded below; re-measure with resources.sh.
#   Rscript plot_resources.R
# ==============================================================================

source("config.R")
suppressMessages(library(ggplot2))
OUT <- dirname(OUT_CSV)

OPAL <- "#9AA0A6"; ARMA <- "#4285F4"

# --- resting memory (GiB), idle. Containers (Opal server+Mongo+Rock, Armadillo
# Rock/Rserve engines) via `docker stats` RSS. The Armadillo SERVER is a host JVM
# (not a container), so its full OS-level footprint is measured with macOS
# `sudo vmmap <pid> | grep 'physical footprint'` (0.227 GiB idle / 0.25 peak) --
# NOT the actuator jvm.memory.used, which omits native/off-heap and undercounts.
# Rock and Rserve are alternative Armadillo engines, so they are shown as
# separate stacks. Coloured by aspect (App server / MongoDB / R engine), each
# aspect the same colour across all three stacks. Colours from the slide theme.
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
  labs(title = "Resting memory footprint",
       x = NULL, y = "GiB resident", fill = NULL) +
  scale_y_continuous(limits = c(0, 3), breaks = 0:3, expand = expansion(mult = c(0, 0.03))) +
  theme_minimal(base_size = 13, base_family = "IBM Plex Mono") +
  theme(legend.position = "right", plot.title = element_text(size = 12),
        plot.margin = margin(10, 15, 5, 10))
ggsave(file.path(OUT, "res_memory.png"), p1, width = 6.6, height = 4.2, dpi = 300, device = ragg::agg_png)
cat("Wrote", file.path(OUT, "res_memory.png"), "\n")

# --- data on disk: CNSIM (10k rows x 29 cols), Parquet vs Opal Mongo value_set --
sto <- data.frame(
  backend = c("Opal (Mongo)", "Armadillo (Parquet)"),
  kb      = c(2140, 196)
)
sto$mb <- sto$kb / 1024
sto$backend <- factor(sto$backend, levels = c("Opal (Mongo)", "Armadillo (Parquet)"))
p2 <- ggplot(sto, aes(x = backend, y = mb, fill = backend)) +
  geom_col(width = 0.55) +
  geom_text(aes(label = sprintf("%.2f MB", mb)), vjust = -0.4, size = 4.2, fontface = "bold", colour = "#444") +
  scale_fill_manual(values = c("Opal (Mongo)" = "#0097A7", "Armadillo (Parquet)" = "#0097A7")) +
  scale_x_discrete(labels = function(x) sub(" \\(", "\n(", x)) +
  labs(title = "Data on disk — CNSIM (10,000 rows x 29 cols)",
       x = NULL, y = "MB on disk", fill = NULL) +
  scale_y_continuous(limits = c(0, 3), breaks = 0:3, expand = expansion(mult = c(0, 0.03))) +
  theme_minimal(base_size = 13, base_family = "IBM Plex Mono") +
  theme(legend.position = "none", plot.title = element_text(size = 12),
        plot.margin = margin(10, 15, 5, 10))
ggsave(file.path(OUT, "res_storage.png"), p2, width = 6.6, height = 4.2, dpi = 300, device = ragg::agg_png)
cat("Wrote", file.path(OUT, "res_storage.png"), "\n")
