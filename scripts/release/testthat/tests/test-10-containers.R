# Setup
test_name <- "containers"

create_ds_connection <- function(container = "") {
  if (release_env$ADMIN_MODE) {
    con <- dsConnect(
      drv = armadillo(),
      name = "armadillo",
      user = "admin",
      password = release_env$admin_pwd,
      url = release_env$armadillo_url,
      profile = container
    )
  } else {
    con <- dsConnect(
      drv = armadillo(),
      name = "armadillo",
      token = release_env$token,
      url = release_env$armadillo_url,
      profile = container
    )
  }
  return(con)
}

test_that("connect to specified container", {
  do_skip_test(test_name)
  con <- create_ds_connection(container = release_env$current_container)
  expect_equal(con@name, "armadillo")
  dsDisconnect(con)
})

test_that("connect without specifying container", {
  do_skip_test(test_name)
  con <- create_ds_connection(container = "")
  expect_equal(con@name, "armadillo")
  dsDisconnect(con)
})

test_that("connect to default container", {
  do_skip_test(test_name)
  con <- create_ds_connection(container = "default")
  expect_equal(con@name, "armadillo")
  dsDisconnect(con)
})