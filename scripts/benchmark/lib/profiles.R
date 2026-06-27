# ==============================================================================
# Ensure the benchmark's two compute profiles exist and are running on the local
# Armadillo: `default` (built-in) and `rserve`, each backed by ROCK_IMAGE -- the
# published, dsBase-pinned datashield/rock-base:<version>. This is the "three-way
# version honesty" that makes Opal-vs-Armadillo valid: Opal's Rock, both Armadillo
# profiles, and the client lib all carry the same dsBase.
#
# Lean, self-contained adaptation of scripts/release/lib/setup-profiles.R
# (create_profile / start_profile via the ds-profiles API), best-effort: if the
# API rejects a create (schema drift across Armadillo versions), it prints a clear
# message so you can create the profile in the UI and re-run, rather than aborting.
#
#   ARMA_AUTH=basic Rscript lib/profiles.R
# ==============================================================================

suppressMessages(library(httr))

ARMA_URL   <- sub("/+$", "", Sys.getenv("ARMA_URL", "http://localhost:8081"))
ARMA_USER  <- Sys.getenv("ARMA_USER", "admin")
ARMA_PASS  <- Sys.getenv("ARMA_PASS", "admin")
ROCK_IMAGE <- Sys.getenv("ROCK_IMAGE", "datashield/rock-base:latest")
PROFILES   <- trimws(strsplit(Sys.getenv("BENCH_PROFILES", "default,rserve"), ",")[[1]])

auth <- httr::authenticate(ARMA_USER, ARMA_PASS)
api  <- function(path) paste0(ARMA_URL, "/", path)

list_profiles <- function() {
  r <- httr::GET(api("ds-profiles"), auth)
  if (httr::status_code(r) != 200)
    stop("GET ds-profiles failed (", httr::status_code(r), ")", call. = FALSE)
  vapply(httr::content(r), function(p) p$name, character(1))
}

# Create a profile on the dsBase-pinned Rock image, with PERMISSIVE disclosure set
# as a profile option (datashield.privacyControlLevel) -- this is why we can use
# the stock pinned image instead of a custom permissive build. The benchmark needs
# permissive for ops like ds.recodeValues / ds.Boole.
create_profile <- function(name) {
  body <- list(name = name, image = ROCK_IMAGE,
               packageWhitelist = list("dsBase"), functionBlacklist = list(),
               options = list(datashield.privacyControlLevel = "permissive"))
  r <- httr::PUT(api("ds-profiles"), auth, body = body, encode = "json")
  if (!httr::status_code(r) %in% c(200L, 204L))
    stop(sprintf("create profile '%s' failed (%s) -- create it in the UI (image %s, permissive) and re-run",
                 name, httr::status_code(r), ROCK_IMAGE), call. = FALSE)
}

start_profile <- function(name) {
  r <- httr::POST(api(paste0("ds-profiles/", name, "/start")), auth)
  if (!httr::status_code(r) %in% c(200L, 204L, 409L))   # 409 = already running
    stop(sprintf("start profile '%s' failed (%s)", name, httr::status_code(r)), call. = FALSE)
}

existing <- list_profiles()
for (p in PROFILES) {
  if (!p %in% existing) {
    cat(sprintf("  creating profile '%s' (image %s)\n", p, ROCK_IMAGE))
    create_profile(p)
  }
  cat(sprintf("  starting profile '%s'\n", p))
  start_profile(p)
}
cat(sprintf("Profiles ready: %s\n", paste(PROFILES, collapse = ", ")))
