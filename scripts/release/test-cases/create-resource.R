make_resource <- function(target_project, url, skip_tests) {
    test_name <- "make_resource"
    if(do_skip_test(test_name, skip_tests)) {return()}

    rds_url <- url
    if(url == "http://localhost:8080/") {
        rds_url <- "http://host.docker.internal:8080/"
    }

    created_resource <- resourcer::newResource(
      name = "GSE66351_1",
      url = sprintf("%sstorage/projects/%s/objects/ewas%sgse66351_1.rda", rds_url, target_project,"%2F"),
      format = "ExpressionSet"
    )
    cli_alert_success("Resource created")
    return(created_resource)
}
