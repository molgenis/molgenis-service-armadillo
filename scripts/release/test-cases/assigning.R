check_tables_assign <- function(project, folder, table) {
    cli_alert_info(sprintf("Assigning table %s", table))
    datashield.assign.table(conns, table, sprintf("%s/%s/%s", project, folder, table))
    datatype <- ds.class(x = table, datasources = conns)
    expected_type <- list()
    expected_type$armadillo = "data.frame"

    if (identical(datatype, expected_type)){
        cli_alert_success("Assigned table is dataframe")
    } else {
        cli_alert_danger("Assigned table not of expected type:")
        print(datatype)
    }
}

check_expression_assign <- function(project, object, variable) {
    cli_alert_info(sprintf("Assigning expression for %s$%s", object, variable))
    datashield.assign.expr(conns, "x", expr = as.symbol(paste0(object, "$", variable)))
    cli_alert_success("Expression assigned")
}

check_assigning <- function(project, folder, table, object, variable, skip_tests) {
    test_name <- "assigning"
    if(any(skip_tests %in% test_name)){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

    check_tables_assign(project, folder, table)
    check_expression_assign(project, object, variable)
    }

