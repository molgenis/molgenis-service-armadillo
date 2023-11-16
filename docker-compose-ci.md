# docker-compose-ci.yml

## R Base
To make sure the ci works check

```sh
docker run -it --entrypoint /bin/bash r-base:latest
# run your R queries like
Rscript --version
# Rscript (R) version 4.3.2 (2023-10-31)
installed.packages()[, "Package"]
```

### Analyse

```
apt update
apt install --yes procps

ps -aef
```

## cicd

```sh
docker-compose --file docker-compose-ci.yml up --detach cicd
docker-compose --file docker-compose-ci.yml ps
docker-compose --file docker-compose-ci.yml exec cicd bash
```

### Hacks

```sh
apt update
apt upgrade

# devtools break
apt --yes install libcurl4-gnutls-dev
```

```R
source(./install_release_script_dependencies.R)

> warnings()
Warning messages:
1: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘openssl’ had non-zero exit status
2: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘systemfonts’ had non-zero exit status
3: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘xml2’ had non-zero exit status
4: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘credentials’ had non-zero exit status
5: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘httr2’ had non-zero exit status
6: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘textshaping’ had non-zero exit status
7: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘httr’ had non-zero exit status
8: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘roxygen2’ had non-zero exit status
9: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘rversions’ had non-zero exit status
10: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘urlchecker’ had non-zero exit status
11: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘gert’ had non-zero exit status
12: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘gh’ had non-zero exit status
13: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘ragg’ had non-zero exit status
14: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘usethis’ had non-zero exit status
15: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘pkgdown’ had non-zero exit status
16: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘devtools’ had non-zero exit status
17: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘openssl’ had non-zero exit status
18: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘httr’ had non-zero exit status
19: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘resourcer’ had non-zero exit status
20: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘openssl’ had non-zero exit status
21: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘httr’ had non-zero exit status
22: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘MolgenisAuth’ had non-zero exit status
23: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘MolgenisArmadillo’ had non-zero exit status
24: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘openssl’ had non-zero exit status
25: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘httr’ had non-zero exit status
26: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘MolgenisAuth’ had non-zero exit status
27: In install.packages(pkg, repos = "http://cran.us.r-project.org",  ... :
  installation of package ‘DSMolgenisArmadillo’ had non-zero exit status
28: In eval(ei, envir) : 
```

```
grep "installation of pac" *.md |sort -u | cut -c 49- | cut  -f 1 -d ' ' | echo
```

openssl systemfonts xml2 credentials httr2
textshaping httr roxygen2 rversions urlchecker
gert’
gh’
ragg’
usethis’
pkgdown’
devtools’
openssl’
httr’
resourcer’
openssl’
httr’
MolgenisAuth’
MolgenisArmadillo’
openssl’
httr’
MolgenisAuth’
DSMolgenisArmadillo’
# Misc

Some result when running `install_release_script_dependencies.R`

```sh

...

ℹ Installing packages
ℹ Installing getPass
also installing the dependency ‘rstudioapi’

ℹ Installing arrow
also installing the dependencies ‘bit’, ‘lifecycle’, ‘magrittr’, ‘withr’, ‘assertthat’, ‘bit64’, ‘glue’, ‘purrr’, ‘R6’, ‘rlang’, ‘tidyselect’, ‘vctrs’, ‘cpp11’

...

ℹ Installing jsonlite
ℹ Installing future
also installing the dependencies ‘digest’, ‘globals’, ‘listenv’, ‘parallelly’

ℹ Installing MolgenisArmadillo
also installing the dependencies ‘sys’, ‘askpass’, ‘utf8’, ‘curl’, ‘mime’, ‘openssl’, ‘Rcpp’, ‘triebeard’, ‘generics’, ‘pillar’, ‘stringi’, ‘fansi’, ‘pkgconfig’, ‘base64enc’, ‘httr’, ‘urltools’, ‘dplyr’, ‘stringr’, ‘tidyr’, ‘tibble’, ‘MolgenisAuth’

ℹ Installing DSI
also installing the dependencies ‘hms’, ‘prettyunits’, ‘crayon’, ‘progress’

ℹ Installing devtools
also installing the dependencies ‘credentials’, ‘openssl’, ‘zip’, ‘gitcreds’, ‘httr2’, ‘ini’, ‘fastmap’, ‘httpuv’, ‘xtable’, ‘fontawesome’, ‘sourcetools’, ‘later’, ‘promises’, ‘jquerylib’, ‘sass’, ‘systemfonts’, ‘textshaping’, ‘tinytex’, ‘xfun’, ‘highr’, ‘diffobj’, ‘rematch2’, ‘clipr’, ‘curl’, ‘gert’, ‘gh’, ‘rappdirs’, ‘rprojroot’, ‘whisker’, ‘yaml’, ‘cachem’, ‘shiny’, ‘htmltools’, ‘callr’, ‘processx’, ‘bslib’, ‘downlit’, ‘httr’, ‘ragg’, ‘rmarkdown’, ‘xml2’, ‘htmlwidgets’, ‘xopen’, ‘brew’, ‘commonmark’, ‘knitr’, ‘brio’, ‘evaluate’, ‘praise’, ‘ps’, ‘waldo’, ‘usethis’, ‘desc’, ‘ellipsis’, ‘fs’, ‘memoise’, ‘miniUI’, ‘pkgbuild’, ‘pkgdown’, ‘pkgload’, ‘profvis’, ‘rcmdcheck’, ‘remotes’, ‘roxygen2’, ‘rversions’, ‘sessioninfo’, ‘testthat’, ‘urlchecker’
```
