package org.molgenis.armadillo.container;

import org.molgenis.armadillo.controller.ContainerDockerController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContainerSchedulerFallbackConfig {

  @Bean
  @ConditionalOnProperty(
      value = ContainerDockerController.DOCKER_MANAGEMENT_ENABLED,
      havingValue = "false",
      matchIfMissing = true // Also covers missing property
      )
  public ContainerScheduler noOpContainerScheduler() {
    return new ContainerScheduler(null, null) {
      @Override
      public void reschedule(DatashieldContainerConfig container) {
        // no-op
      }

      @Override
      public void cancel(String containerName) {
        // no-op
      }
    };
  }
}
