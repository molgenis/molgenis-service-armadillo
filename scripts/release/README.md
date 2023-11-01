# Release

(this is a document in progress)

When releasing we run our `release-test.R` script to

- populate Armadillo with data
- add docker DataShield compatible images
- test Armadillo API calls through `DSMolgenisArmadillo` R package
- test DataShield querys against a running docker container

After running the `release-test.R` the server could still contain test data so cleanup.

## .env file

To prefill answers needed to run the test you can use one of the `*.env.dist`
to copy into `.env`. It's settings will be used when needed. This makes switching between ie site, user a little easier.

# Modules

This list some modules important to know about.

The text below is still mostly incorrect but it's a start do digest `release-test.R`.

## DSMolgenisArmadillo

This is a DataShield Driver for communicating with Armadillo.

### Insight

The name `armadillo` provided is confusing as most other follow the data shield naming convention: "ds*" for client methods.

```zsh
grep "ds[A-Z]{1}.*(" release-test.R
```

```zsh
grep "armadillo\..*(" release-test.R
```

## MolgenisArmadillo

Wrapper around the Armadillo API

## DSI

The Data Shield Interface with alias `datashield`.

### Insight

```zsh
grep "armadillo\..*(" release-test.R
```

# Wishlist

- split up `test-release.R` to get less lines, follow DRY and add test selection.
- use `lintr`
- add `testthat`?

## lintr

Helps produce better code lines

```R
install.packages("lintr")
library(lintr)
?lintr
lintr::lint_dir()
```