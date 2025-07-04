package org.molgenis.armadillo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "org.molgenis")
@EnableRetry
@EnableScheduling
public class ArmadilloServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ArmadilloServiceApplication.class, args);
  }
}
