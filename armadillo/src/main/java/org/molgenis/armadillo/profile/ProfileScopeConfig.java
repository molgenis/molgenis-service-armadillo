package org.molgenis.armadillo.profile;

import static java.lang.String.format;
import static org.molgenis.armadillo.profile.ActiveProfileNameAccessor.getActiveProfileName;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import org.molgenis.armadillo.exceptions.UnknownProfileException;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServerConnectionFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileScopeConfig {

  @Bean
  @org.molgenis.armadillo.profile.annotation.ProfileScope
  public ProfileConfig profileConfig(ProfileService profileService) {
    var activeProfileName = getActiveProfileName();
    try {
      return runAsSystem(() -> profileService.getByName(activeProfileName));
    } catch (UnknownProfileException e) {
      throw new IllegalStateException(
          format("Missing profile configuration for active profile '%s'.", activeProfileName));
    }
  }

  @Bean
  @org.molgenis.armadillo.profile.annotation.ProfileScope
  public RConnectionFactory rConnectionFactory(ProfileConfig profileConfig) {
    return new RServerConnectionFactory(profileConfig.toEnvironmentConfigProps());
  }

  @Bean
  public static BeanFactoryPostProcessor beanFactoryPostProcessor(ProfileScope profileScope) {
    return beanFactory -> beanFactory.registerScope("profile", profileScope);
  }
}
