#!/usr/bin/env Rscript

if (!require(rstudioapi)) {
  install.packages("rstudioapi")
}

if (rstudioapi::isAvailable()) {
  print("... from RStudio")
  script_path = rstudioapi::getSourceEditorContext()$path
} else {
  print("... from shell")
  source("src/lib/rstudioapi.R")
  script_path = get_script_path()
}

script_dir = paste0(dirname(script_path), '/')
cat(paste0("\nRunning ", script_path), "\n")

# @see https://github.com/obiba/rockr
library(rockr)

readRenviron(paste0(script_dir, "/../.env"))

rock_url = Sys.getenv("ROCK_URL")
rock_admin = Sys.getenv("ROCK_ADMIN")
rock_password = Sys.getenv("ROCK_PASSWORD")
DEBUG <- Sys.getenv("DEBUG", "") == "1"

cat(paste0("Testing ", rock_url, " as admin user ", rock_admin, "\n\n"))

make_conn <- function() {
  conn <- rockr.connect(username=rock_admin, password=rock_password, url = rock_url)
  return(conn)
}

conn <- make_conn()

start <- function() {
  cat('Starting ...\n')
  rockr.start(conn)
}

stop <- function() {
  cat("Stopping ...\n")
  rockr.stop(conn)
}

status <- function() {
  cat("\nStatus ...")
  rstats = rockr.status(conn)
  print(rstats$sessions$total)
}

restart <- function() {
  stop()
  start()
}

test <- function() {
  if (exists("conn$session")) {
    conn.close()
  }
  conn <- make_conn()
  rockr.open(conn)
  cat("LS")
  cat(rockr.eval(conn, quote(ls())))
  cat(rockr.eval(conn, call("ls")))
  
  # script <- "library(magrittr);ps::ps() %>% dplyr::filter(grepl("Rserve", name)) %>% dplyr::count()'"
  #    rockr.eval(conn, call("library(magrittr);ps::ps()"))
  cat("< LS")
}

sessions <- function() {
  sessions <- rockr.sessions(conn)
  for ( s in sessions) {
    print(s$id)
  }
  sessions
}

close_sessions <- function() {
  sessions <- rockr.sessions(conn)
  for ( s in sessions) {
    rockr.session_switch(conn, s)
    rockr.close(conn)
  }
}


packages <- function() {
  packages <- rockr.packages(conn)
}

eval <- function(script) {
  conn <- make_conn()
  rockr.open(conn)
  rockr.eval(conn, script)
}

consume_memory <- function(n = 10) {
  for(i in 1:n) {
    cat(paste0("\n", "Making connection ", i, " out of ", n))
    make_conn()
    test()
  }
}

poll_status <- function(n) {
  for ( i in 1:n) {
    status()
    Sys.sleep(10)
  }
}

rock_log <- function() {
  rockr.log(conn)
}

version <- function() {
  rockr.version(conn)
}

print_debug <- function(s) {
  if (DEBUG) {
    cat(s, "\n")
  }
}

print_debug("DEBUG ON")

commands <- c(
  "start", # R Server
  "restart",
  "stop",

  "rock_log", # Rock
  "close_sessions",
  "version", 
  "sessions", # Users
  "test",

  "packages", # info
  "packages_datashield",
  "poll_status",
  "status",
  "consume_memory",
  "eval"
  )

# args: command rest...
execute <- function(args) {
  print_debug(paste0("Running ... ", args[1]))

  args <- args[-1]
  
  result <- do.call(cmd, args)
  print(result)
}

args <- commandArgs(trailingOnly = TRUE)
cmd <- "-"

if (length(args) > 0) {
  cmd <- args[1]
  print_debug(paste0("Arguments ... ", cmd))
}

if (cmd %in% commands) {
  print_debug(paste0("Running ... ", cmd))
  args <- as.list(args)
  execute(args)
} else {
  cat(c("Valid commands are\n- "), "\n") 
  cat(commands, sep = "\n- ")
}

rockr.close(conn)

