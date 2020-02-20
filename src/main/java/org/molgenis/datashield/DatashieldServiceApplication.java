package org.molgenis.datashield;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class DatashieldServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(DatashieldServiceApplication.class, args);
  }
}
