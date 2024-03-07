make_resource <- function(target_project) {

    rds_url <- armadillo_url
    if(armadillo_url == "http://localhost:8080/") {
        rds_url <- "http://host.docker.internal:8080/"
    }

    created_resource <- resourcer::newResource(
      name = "GSE66351_1",
      url = sprintf("%sstorage/projects/%s/objects/ewas%sgse66351_1.rda", rds_url, target_project,"%2F"),
      format = "ExpressionSet"
    )

    return(created_resource)
}
