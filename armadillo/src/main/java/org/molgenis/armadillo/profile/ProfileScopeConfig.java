package org.molgenis.armadillo.profile;

import static org.molgenis.armadillo.profile.ActiveProfileNameAccessor.getActiveProfileName;

import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RConnectionFactoryImpl;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileScopeConfig {
  @Bean
  @org.molgenis.armadillo.profile.annotation.ProfileScope
  public ProfileConfig profileConfig(ProfileService profileService) {
    var activeProfileName = getActiveProfileName();
    return profileService.getAll().stream()
        .filter(it -> it.getName().equals(activeProfileName))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Missing profile configuration for active profile '"
                        + activeProfileName
                        + "'."));
  }

  @Bean
  @org.molgenis.armadillo.profile.annotation.ProfileScope
  public RConnectionFactory rConnectionFactory(ProfileConfig profileConfig) {
    return new RConnectionFactoryImpl(profileConfig.getEnvironmentConfigProps());
  }

  @Bean
  public BeanFactoryPostProcessor beanFactoryPostProcessor(ProfileScope profileScope) {
    return beanFactory -> beanFactory.registerScope("profile", profileScope);
  }
}
