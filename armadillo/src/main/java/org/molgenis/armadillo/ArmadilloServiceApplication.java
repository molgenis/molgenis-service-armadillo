package org.molgenis.armadillo;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = "org.molgenis")
@EnableRetry
public class ArmadilloServiceApplication {
  private static ConfigurableApplicationContext context;
  private static ApplicationArguments arguments;

  public static void main(String[] args) {
    arguments = new DefaultApplicationArguments(args);
    context = SpringApplication.run(ArmadilloServiceApplication.class, args);
  }

  public static void restart() {
    // programmatically trigger restart, will not reload application.yml
    Thread thread =
        new Thread(
            () -> {
              context.close();
              context =
                  SpringApplication.run(
                      ArmadilloServiceApplication.class, arguments.getSourceArgs());
            });

    thread.setDaemon(false);
    thread.start();
  }
}
