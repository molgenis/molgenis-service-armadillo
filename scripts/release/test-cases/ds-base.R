verify_ds_obtained_mean <- function(ds_mean, expected_mean, expected_valid_and_total) {
  if(! round(ds_mean[1], 3) == expected_mean){
    cli_alert_danger(paste0(ds_mean[1], "!=", expected_mean))
    exit_test("EstimatedMean incorrect!")
  } else if(ds_mean[2] != 0) {
    cli_alert_danger(paste0(ds_mean[2], "!=", 0))
    exit_test("Nmissing incorrect!")
  } else if(ds_mean[3] != expected_valid_and_total) {
    cli_alert_danger(paste0(ds_mean[3], "!=", expected_valid_and_total))
    exit_test("Nvalid incorrect!")
  } else if(ds_mean[4] != expected_valid_and_total) {
    cli_alert_danger(paste0(ds_mean[4], "!=", expected_valid_and_total))
    exit_test("Ntotal incorrect!")
  } else {
    cli_alert_success("Mean values correct")
  }
}

verify_ds_hist <- function(object, variable){
    hist <- ds.histogram(x = paste0(object, "$", variable), datasources = conns)
    breaks <- c(35.31138,116.38319,197.45500,278.52680,359.59861,440.67042,521.74222,602.81403,683.88584,764.95764,846.02945)
    counts <- c(106,101,92,103,106,104,105,101,113,69)
    density <- c(0.0013074829,0.0012458092,0.0011347965,0.0012704787,0.0013074829,0.0012828134,0.0012951481,0.0012458092,0.0013938261,0.0008510974)
    mids <- c(75.84729,156.91909,237.99090,319.06271,400.13451,481.20632,562.27813,643.34993,724.42174,805.49355)
    cli_alert_info("Validating histogram breaks")
    compare_list_values(hist$breaks, breaks)
    cli_alert_info("Validating histogram counts")
    compare_list_values(hist$counts, counts)
    cli_alert_info("Validating histogram density")
    compare_list_values(hist$density, density)
    cli_alert_info("Validating histogram mids")
    compare_list_values(hist$mids, mids)
}

# # compare values in two lists
compare_list_values <- function(list1, list2) {
  vals_to_print <- cli_ul()
  equal <- TRUE
  for (i in 1:length(list1)) {
    val1 <- list1[i]
    val2 <- list2[i]
    if(almost_equal(val1, val2) == TRUE){
      cli_li(sprintf("%s ~= %s", val1, val2))
    } else {
      equal <- FALSE
      cli_li(sprintf("%s != %s", val1, val2))
    }
  }
  cli_end(vals_to_print)
  if(equal){
    cli_alert_success("Values equal")
  } else {
    cli_alert_danger("Values not equal")
  }
}

# theres a bit of noise added in DataSHIELD answers, causing calculations to not always be exactly the same, but close
# here we check if they're equal enough
almost_equal <- function(val1, val2) {
  return(all.equal(val1, val2, tolerance= .Machine$double.eps ^ 0.03))
}

verify_ds_base <- function(object, variable){
    cli_alert_info(sprintf("Verifying mean function works on %s$%s", object, variable))
    ds_mean <- ds.mean(paste0(object, "$", variable), datasources = conns)$Mean
    cli_alert_info("Verifying mean values")
    verify_ds_obtained_mean(ds_mean, 431.105, 1000)
    cli_alert_success("dsMean returns expected values")

    cli_alert_info("Verifying can create histogram")
    verify_ds_hist(object, variable)
    cli_alert_success("ds.histogram returns expected values")

}






