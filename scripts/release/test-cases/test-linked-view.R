cli_alert_info("Testing linked table")
#TODO: replace this by following once implemented in R api:
#linked_data <- armadillo.load_table(link_project, "core-variables", "nonrep")
query <- list(table = paste0(link_project, "/core-variables/nonrep"), symbol = "core_nonrep", async = TRUE)
variables <- c("child_id", "mother_id")
query$variables <- paste(unlist(variables), collapse = ",")
response <- httr::POST(
  handle = handle(armadillo_url),
  path = "/load-table",
  query = query,
  config = httr::add_headers(get_auth_header(auth_type, token))
)

if (!response$status_code == 201) {
  exit_test(sprintf("Unable to retrieve linked object %s/%s from source: %s/%s, status code: %s", link_project, linkObj, project1, srcObj, response$status_code))
} else {
  cli_alert_success(sprintf("Successfully retrieved linked object %s/%s from source: %s/%s with variables %s", link_project, linkObj, project1, srcObj, paste(variables, collapse = ", ")))
}
