# ==============================================================================
# Close orphaned DataSHIELD sessions left behind by killed benchmark runs.
#
#   Rscript cleanup_sessions.R
#
# A hard-killed run never reaches bench.R's logout_all(), so its server-side R
# sessions linger -- each is a live R worker holding a profile slot, so new
# commands can queue/hang (esp. on a shared Opal). Run this before a (re)launch
# to start clean. Opal: GET /ws/datashield/sessions, DELETE each. Armadillo R
# sessions idle-expire; restart the profile containers to force-clear locally.
# ==============================================================================

source("config.R")
suppressMessages(library(httr))

close_opal <- function(spec) {
  base <- sub("/+$", "", spec$url)
  auth <- httr::authenticate(spec$user, spec$pass)
  r <- httr::GET(paste0(base, "/ws/datashield/sessions"), auth, httr::accept_json())
  if (httr::status_code(r) != 200) {
    cat(sprintf("  %-12s list failed (%d)\n", spec$be, httr::status_code(r))); return(invisible())
  }
  ss  <- httr::content(r, as = "parsed", simplifyVector = FALSE)
  ids <- vapply(ss, function(s) s$id, character(1))
  if (!length(ids)) { cat(sprintf("  %-12s no open sessions\n", spec$be)); return(invisible()) }
  for (id in ids) {
    d <- httr::DELETE(paste0(base, "/ws/datashield/session/", id), auth)
    cat(sprintf("  %-12s closed %s (%d)\n", spec$be, id, httr::status_code(d)))
  }
}

for (be in BACKENDS[backend_kind(BACKENDS) == "opal"])
  tryCatch(close_opal(backend_spec(be)),
           error = function(e) message(sprintf("  %s cleanup error: %s", be, conditionMessage(e))))
cat("Session cleanup done.\n")
