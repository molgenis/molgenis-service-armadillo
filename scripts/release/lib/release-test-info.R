show_test_info <- function() {
  test_name <- "show_test_info"
  if (do_skip_test(test_name)) {
    return()
  }

  if (release_env$admin_pwd != "") {
    admin_pwd_msg <- TRUE
  }

  test_message <- cat(sprintf("
                      ,.-----__                       Testing version: %s
                ,:::://///,:::-.                      Test server: %s
               /:''/////// ``:::`;/|/                 OIDC User: %s
              /'   ||||||     :://'`\\                 Admin password set: %s
            .' ,   ||||||     `/(  e \\                Directory for test files: %s
      -===~__-'\\__X_`````\\_____/~`-._ `.              Profile: %s
                  ~~        ~~       `~-'             Admin-only mode: %s
    ", release_env$version, release_env$armadillo_url, release_env$user, admin_pwd_msg,
    release_env$dest, release_env$current_profile, release_env$ADMIN_MODE))

  return(test_message)
}
