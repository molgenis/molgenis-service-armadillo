package org.molgenis.armadillo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(
    scanBasePackages = "org.molgenis",
    exclude = {SecurityAutoConfiguration.class, UserDetailsServiceAutoConfiguration.class})
@EnableRetry
public class ArmadilloServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(ArmadilloServiceApplication.class, args);
  }
}
