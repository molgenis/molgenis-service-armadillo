package org.molgenis.armadillo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = "org.molgenis")
@EnableRetry
public class ArmadilloServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ArmadilloServiceApplication.class, args);
  }
}
