package org.molgenis.armadillo.profile;

import org.molgenis.armadillo.controller.ProfilesDockerController;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileSchedulerFallbackConfig {

  @Bean
  @ConditionalOnProperty(
      value = ProfilesDockerController.DOCKER_MANAGEMENT_ENABLED,
      havingValue = "false",
      matchIfMissing = true // Also covers missing property
      )
  public ProfileScheduler noOpProfileScheduler() {
    return new ProfileScheduler(null, null) {
      @Override
      public void reschedule(ProfileConfig profile) {
        // no-op
      }

      @Override
      public void cancel(String profileName) {
        // no-op
      }
    };
  }
}
