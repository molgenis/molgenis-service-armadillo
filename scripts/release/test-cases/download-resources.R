prepare_resources <- function(rda_dir, skip_tests) {
    test_name <- "prepare-resources"
    if(skip_tests %in% test_name){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

    if(!file.exists(rda_dir)){
      cli_alert_warning("Unable to locate gse66351_1.rda in testing directory, downloading.")
      download.file("https://github.com/isglobal-brge/brge_data_large/raw/master/data/gse66351_1.rda", rda_dir)
    }

    cli_alert_info("Checking if rda dir exists")
    if (rda_dir == "" || !file.exists(rda_dir)) {
      exit_test(sprintf("File [%s] doesn't exist", rda_dir))
    }
    cli_alert_success("Resource ready for testing")
}
