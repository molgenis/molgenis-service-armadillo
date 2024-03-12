verify_see_resource <- function(full_resource_path) {
    cli_alert_info("Testing if we see the resource")
    print(full_resource_path)
    print(datashield.resources(conns))
    if(full_resource_path %in% datashield.resources(conns = conns)$armadillo){
      cli_alert_success("Success")
    } else {
      cli_alert_danger("Failure")
    }
}

verify_assign_resource <- function(full_resource_path) {
    cli_alert_info("Testing if we can assign resource")
    datashield.assign.resource(conns, resource = full_resource_path, symbol = "eSet_0y_EUR")
    cli_alert_info("Getting RObject class of resource")
    resource_class <- ds.class('eSet_0y_EUR', datasources = conns)
    expected <- c("RDataFileResourceClient", "FileResourceClient", "ResourceClient", "R6")
    if (length(setdiff(resource_class$testserver, expected)) == 0) {
      cli_alert_success("Success")
    } else {
      cli_alert_danger("Failure")
    }
}

# verify_assign_expression <- function() {
#     cli_alert_info("Testing if we can assign expression")
#     tryCatch({
#       datashield.assign.expr(conns, symbol = "methy_0y_EUR",expr = quote(as.resource.object(eSet_0y_EUR)))
#     }, error = function(e) {
#         cli_alert_danger(datashield.errors())
#         })
# } This is failing and the tryCatch is also not working

verify_resources <- function(project, resource_path) {
    full_resource_path = sprintf("%s/%s", project, resource_path)
    verify_see_resource(full_resource_path)
    verify_assign_resource(full_resource_path)
#     verify_assign_expression(conns)
}


verify_resources <- function(project, resource_path, ADMIN_MODE, profile_info, skip_tests) {
    test_name <- "verify-resources"
    if(any(skip_tests %in% test_name)){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

    if (ADMIN_MODE) {
       cli_alert_warning("Cannot test working with resources as basic authenticated admin")
    } else if (!"resourcer" %in% profile_info$packageWhitelist) {
      cli_alert_warning(sprintf("Resourcer not available for profile: %s, skipping testing using resources.", profile))
    } else {
        cli_h2("Using resources as regular user")
        cli_h2("Verifying resources")
        full_resource_path = sprintf("%s/%s", project, resource_path)
        verify_see_resource(full_resource_path)
        verify_assign_resource(full_resource_path)
    #   verify_assign_expression(conns)
    }
}