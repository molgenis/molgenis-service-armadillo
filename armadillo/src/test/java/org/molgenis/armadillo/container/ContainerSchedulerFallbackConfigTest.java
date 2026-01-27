package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.controller.ContainerDockerController;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ContainerSchedulerFallbackConfigTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(ContainerSchedulerFallbackConfig.class);

  @Test
  void noOpSchedulerCreatedWhenPropertyMissing() {
    contextRunner.run(
        context -> {
          var schedulers = context.getBeansOfType(ContainerScheduler.class);
          assertEquals(1, schedulers.size());
          var scheduler = schedulers.values().iterator().next();
          assertDoesNotThrow(() -> scheduler.reschedule(null));
          assertDoesNotThrow(() -> scheduler.cancel("test"));
        });
  }

  @Test
  void noOpSchedulerCreatedWhenPropertyFalse() {
    contextRunner
        .withPropertyValues(ContainerDockerController.DOCKER_MANAGEMENT_ENABLED + "=false")
        .run(
            context -> {
              var schedulers = context.getBeansOfType(ContainerScheduler.class);
              assertEquals(1, schedulers.size());
              var scheduler = schedulers.values().iterator().next();
              assertDoesNotThrow(() -> scheduler.reschedule(null));
              assertDoesNotThrow(() -> scheduler.cancel("test"));
            });
  }

  @Test
  void noOpSchedulerNotCreatedWhenPropertyTrue() {
    contextRunner
        .withPropertyValues(ContainerDockerController.DOCKER_MANAGEMENT_ENABLED + "=true")
        .run(context -> assertEquals(0, context.getBeansOfType(ContainerScheduler.class).size()));
  }
}
