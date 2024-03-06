interactive_test <- function(project1) {
    cat("\nNow open your testserver in the browser")
    cat(sprintf("\n\nVerify [%s] is available", project1))
    wait_for_input()
    cat("\nClick on the icon next to the name to go to the project explorer")
    wait_for_input()
    cat("\nVerify the 1_1-outcome-1_0 and 2_1-core-1_0 folders are there")
    wait_for_input()
    cat("\nVerify core contains nonrep, yearlyrep, monthlyrep and trimesterrep")
    wait_for_input()
    cat("\nVerify outcome contains nonrep and yearlyrep")
    wait_for_input()

    if (interactive) {
      cat("\nWere the manual tests successful? (y/n) ")
      success <- readLines("stdin", n=1)
      if(success != "y"){
        cli_alert_danger("Manual tests failed: problem in UI")
        exit_test("Some values incorrect in UI projects view")
      }
    }
}