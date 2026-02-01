verify_see_resource <- function(full_resource_path) {
  cli_alert_info("Testing if we see the resource")
  print(full_resource_path)
  print(datashield.resources(release_env$conns))
  if (full_resource_path %in% datashield.resources(conns = release_env$conns)$armadillo) {
    cli_alert_success("Success")
  } else {
    cli_alert_danger("Failure")
  }
}

verify_assign_resource <- function(full_resource_path) {
  cli_alert_info("Testing if we can assign resource")
  datashield.assign.resource(release_env$conns, resource = full_resource_path, symbol = "eSet_0y_EUR")
  cli_alert_info("Getting RObject class of resource")
  resource_class <- ds.class("eSet_0y_EUR", datasources = release_env$conns)
  expected <- c("RDataFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
  if (length(setdiff(resource_class$testserver, expected)) == 0) {
    cli_alert_success("Success")
  } else {
    cli_alert_danger("Failure")
  }
}

verify_assign_expression <- function() {
  cli_alert_info("Testing if we can assign expression")
  tryCatch(
    {
      datashield.assign.expr(release_env$conns, symbol = "methy_0y_EUR", expr = quote(as.resource.object(eSet_0y_EUR)))
    },
    error = function(e) {
      cli_alert_danger(datashield.errors())
    }
  )
}

verify_resources <- function(resource_path) {
  test_name <- "verify-resources"
  if (do_skip_test(test_name)) {
    return()
  }

  if (release_env$ADMIN_MODE) {
    cli_alert_warning("Cannot test working with resources as basic authenticated admin")
  } else if (!"resourcer" %in% release_env$profile_info$packageWhitelist) {
    cli_alert_warning(sprintf("Resourcer not available for profile: %s, skipping testing using resources.", release_env$current_profile))
  } else {
    cli_h2("Using resources as regular user")
    cli_h2("Verifying resources")
    full_resource_path <- sprintf("%s/%s", release_env$project1, resource_path)
    verify_see_resource(full_resource_path)
    verify_assign_resource(full_resource_path)
    #       verify_assign_expression() FIX ME: ISSUE #699
  }
  cli_alert_success(sprintf("%s passed!", test_name))
}
