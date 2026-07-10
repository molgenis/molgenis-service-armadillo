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

# --- resting memory (GiB), docker stats + Armadillo JVM heap via /actuator -----
mem <- data.frame(
  stack     = c("Opal", "Opal", "Opal", "Armadillo", "Armadillo", "Armadillo"),
  component = c("server", "Mongo", "R engine", "server (JVM)", "Rock engine", "Rserve engine"),
  gib       = c(1.576, 0.242, 0.818, 0.236, 0.479, 0.165)
)
mem$stack <- factor(mem$stack, levels = c("Opal", "Armadillo"))
tot <- tapply(mem$gib, mem$stack, sum)
p1 <- ggplot(mem, aes(x = stack, y = gib, fill = component)) +
  geom_col(width = 0.62) +
  geom_text(data = data.frame(stack = names(tot), gib = as.numeric(tot)),
            aes(x = stack, y = gib, label = sprintf("%.1f GiB", gib)),
            vjust = -0.4, inherit.aes = FALSE, size = 3.6, fontface = "bold") +
  scale_fill_brewer(palette = "Set2") +
  labs(title = "Resting memory footprint", subtitle = "idle, 2-core/8 GB; stacked by component",
       x = NULL, y = "GiB resident", fill = NULL) +
  expand_limits(y = max(tot) * 1.12) +
  theme_minimal(base_size = 12) + theme(legend.position = "right")
ggsave(file.path(OUT, "res_memory.png"), p1, width = 6.2, height = 4.2, dpi = 150)
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
  geom_text(aes(label = sprintf("%.2f MB", mb)), vjust = -0.4, size = 4, fontface = "bold", colour = "#444") +
  scale_fill_manual(values = c("Opal (Mongo)" = OPAL, "Armadillo (Parquet)" = ARMA)) +
  labs(title = "Data on disk — CNSIM (10,000 rows x 29 cols)",
       subtitle = "Parquet columnar + compressed vs Mongo one BSON document per row -- ~11x smaller",
       x = NULL, y = "MB on disk", fill = NULL) +
  expand_limits(y = max(sto$mb) * 1.15) +
  theme_minimal(base_size = 12) + theme(legend.position = "none")
ggsave(file.path(OUT, "res_storage.png"), p2, width = 6.0, height = 4.2, dpi = 150)
cat("Wrote", file.path(OUT, "res_storage.png"), "\n")
