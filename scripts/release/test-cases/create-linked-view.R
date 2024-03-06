# cli_h2()
# #TODO: replace with R code once that is created and released
# auth_header <- get_auth_header(auth_type, token)
# link_project <- generate_random_project_name(available_projects)
# armadillo.create_project(link_project)
# srcObj <- "2_1-core-1_0/nonrep"
# linkObj <- "core-variables/nonrep"
# json_body <- jsonlite::toJSON(
#   list(sourceObjectName = srcObj,
#        sourceProject = project1,
#        linkedObject = linkObj,
#        variables = "child_id,mother_id,row_id,ethn1_m"), auto_unbox=TRUE)
# post_url <- sprintf("%sstorage/projects/%s/objects/link", armadillo_url, link_project)
# response <- POST(post_url,
#                  body=json_body,
#                  encode="json",
#                  config = c(httr::content_type_json(), httr::add_headers(auth_header)))
# if (response$status_code != 204) {
#   exit_test(sprintf("Unable to create linked object %s/%s from source: %s/%s, status code: %s, message: %s", link_project, linkObj, project1, srcObj, response$status_code, response$message))
# } else {
#   cli_alert_success(sprintf("Successfully created linked object %s/%s from source: %s/%s", link_project, linkObj, project1, srcObj))
# }
#