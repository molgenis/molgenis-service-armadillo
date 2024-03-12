verify_mediate_class <- function() {

  ds.glmSLMA(formula = 'agebirth_m_y ~ ethn3_m + sex', family = 'gaussian', dataName = 'nonrep',
  newobj = 'med.fit.1a')

  ds.glmSLMA(formula = 'preg_dia ~ agebirth_m_y + ethn3_m + sex', family = 'gaussian',dataName = 'nonrep',
  newobj = 'out.fit.1a')

  med_out <- ds.mediate(model.m = 'med.fit.1a', model.y = 'out.fit.1a', treat = "ethn3_m", mediator = "agebirth_m_y",
  boot = FALSE, conf.level = 0.95, robustSE = TRUE, sims = 100, seed = 123, newobj = 'med.out.1a')

  med_class <- ds.class("med.out.1a")

  if(med_class == "mediate"){
    cli_alert_success("ds.mediate passed")
  } else{
    cli_alert_danger("ds.mediate failed")
    exit_test("ds.mediate did not return the expected class")
    }
}

verify_ne_weight_class <- function() {
  ds.glmSLMA(formula = 'agebirth_m_y ~ ethn3_m + sex', family = 'gaussian', dataName = 'nonrep',
             newobj = 'med.fit.1b')

  ds.neWeight(object = 'med.fit.1b', newobj = 'expData')

  med_class <- ds.class("expData")

  if(identical(med_class$armadillo, c("data.frame", "expData", "weightData"))){
    cli_alert_success("ds.neWeight passed")
  } else{
    cli_alert_danger("ds.neWeight failed")
    exit_test("ds.neWeight did not return the expected class")
  }

}

verify_ne_model_class <- function() {

  med.out.1b <- ds.neModel(formula = 'preg_dia ~ ethn3_m0 + ethn3_m1 + sex',
                           family = 'gaussian', se = 'robust', expData = 'expData',
                           newobj = 'med.out.1b')

  med_class <- ds.class("med.out.1b")

  if(med_class == "neModel"){
    cli_alert_success("ds.neModel passed")
  } else{
    cli_alert_danger("ds.neModel failed")
    exit_test("ds.neModel did not return the expected class")

  }

}

verify_ne_imp_class <- function() {

  out.fit.1c <- ds.glmSLMA(formula = 'preg_dia ~ agebirth_m_y + ethn3_m + sex',
                           family = 'gaussian', dataName = 'nonrep', newobj ='out.fit.1c')

  ds.neImpute(object = 'out.fit.1c', nMed = 1, newobj = 'impData')

  med_class <- ds.class("impData")

  if(identical(med_class$armadillo, c("data.frame", "expData", "impData"))){
    cli_alert_success("ds.neImpute passed")
  } else{
    cli_alert_danger("ds.neImpute failed")
    exit_test("ds.neImpute did not return the expected class")
  }

}

verify_ne_lht_class <- function() {

  lht.out.1b <- ds.neLht(model = "med.out.1b", linfct = c('ethn3_m0=0', 'ethn3_m1=0', 'ethn3_m0+ethn3_m1=0'))

  med_class <- class(lht.out.1b$armadillo)

  if(med_class == "summary.neLht"){
    cli_alert_success("ds.neLht passed")
  } else{
    cli_alert_danger("ds.neLht failed")
    exit_test("ds.neLht did not return the expected class")
  }

}

verify_ds_mediation <- function(skip_tests) {
  test_name <- "xenon-mediate"
    if(any(skip_tests %in% test_name)){
    return(cli_alert_info(sprintf("Test '%s' skipped", test_name)))
    }

    verify_mediate_class()
    verify_ne_weight_class()
    verify_ne_model_class()
    verify_ne_imp_class()
    verify_ne_lht_class()
}
