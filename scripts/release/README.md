# Release test

The release test script runs against a Armadillo api executing R commands against the running Rock images.

First you need to install the R dependecies

1. Install R `apt install r`
1. Run [install_release_script_dependencies](./install_release_script_dependencies.R).
  `./install_release_script_dependencies.R`
  this may take a while as most dependencies needs a C compilation.
1. Download `dist.env`
1. Copy this to `.env` (a dot env file)
1. Run `./release-test.R`
