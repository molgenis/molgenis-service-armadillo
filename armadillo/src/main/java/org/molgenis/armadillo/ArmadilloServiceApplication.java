package org.molgenis.armadillo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = "org.molgenis")
@EnableRetry
public class ArmadilloServiceApplication {

  public static void main(String[] args) {
    // Allow URL encoded slashes in Tomcat
    System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");

    SpringApplication.run(ArmadilloServiceApplication.class, args);
  }
}
