show_test_info <- function(version, url, user, admin_pwd, dest, profile, ADMIN_MODE, skip_tests) {
    test_name <- "show_test_info"
    if(do_skip_test(test_name, skip_tests)) {return()}

    if(admin_pwd != ""){
    admin_pwd_msg = TRUE
    }

    test_message <- cat(sprintf("
                      ,.-----__                       Testing version: %s
                ,:::://///,:::-.                      Test server: %s
               /:''/////// ``:::`;/|/                 OIDC User: %s
              /'   ||||||     :://'`\\                 Admin password set: %s
            .' ,   ||||||     `/(  e \\                Directory for test files: %s
      -===~__-'\\__X_`````\\_____/~`-._ `.              Profile: %s
                  ~~        ~~       `~-'             Admin-only mode: %s
    ", version, url, user, admin_pwd_msg, dest, profile, ADMIN_MODE))

    return(test_message)
}
