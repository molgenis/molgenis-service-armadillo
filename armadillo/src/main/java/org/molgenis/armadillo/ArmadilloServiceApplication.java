package org.molgenis.armadillo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication(scanBasePackages = "org.molgenis")
@EnableRetry
public class ArmadilloServiceApplication {
  private static final Logger LOGGER = LoggerFactory.getLogger(ArmadilloServiceApplication.class);
  private static ConfigurableApplicationContext context;

  public static void main(String[] args) {
    LOGGER.info("Application starting");
    context = SpringApplication.run(ArmadilloServiceApplication.class, args);
  }

  public static void restart() {
    LOGGER.info("Application restarting");
    ApplicationArguments args = context.getBean(ApplicationArguments.class);
    Thread thread =
        new Thread(
            () -> {
              context.close();
              context =
                  SpringApplication.run(ArmadilloServiceApplication.class, args.getSourceArgs());
            });

    thread.setDaemon(false);
    thread.start();
  }
}
