cat(sprintf("
                  ,.-----__                       Testing version: %s
            ,:::://///,:::-.                      Test server: %s
           /:''/////// ``:::`;/|/                 OIDC User: %s
          /'   ||||||     :://'`\\                 Admin password set: %s
        .' ,   ||||||     `/(  e \\                Directory for test files: %s
  -===~__-'\\__X_`````\\_____/~`-._ `.              Profile: %s
              ~~        ~~       `~-'             Admin-only mode: %s
", version, armadillo_url, user, admin_pwd != "", dest, profile, ADMIN_MODE))
