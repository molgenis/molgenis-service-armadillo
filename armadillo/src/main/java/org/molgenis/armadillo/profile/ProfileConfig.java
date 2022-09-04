package org.molgenis.armadillo.profile;

import static org.molgenis.armadillo.profile.ActiveProfileNameAccessor.getActiveProfileName;

import org.molgenis.armadillo.config.DataShieldConfigProps;
import org.molgenis.armadillo.config.ProfileConfigProps;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RConnectionFactoryImpl;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProfileConfig {
  @Bean
  @org.molgenis.armadillo.config.annotation.ProfileScope
  public ProfileConfigProps profileConfigProps(DataShieldConfigProps dataShieldConfigProps) {
    var activeProfileName = getActiveProfileName();
    return dataShieldConfigProps.getProfiles().stream()
        .filter(it -> it.getName().equals(activeProfileName))
        // todo
        .map(it -> new ProfileConfigProps())
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Missing profile configuration for active profile '"
                        + activeProfileName
                        + "'."));
  }

  @Bean
  @org.molgenis.armadillo.config.annotation.ProfileScope
  public RConnectionFactory rConnectionFactory(EnvironmentConfigProps environmentConfigProps) {
    return new RConnectionFactoryImpl(environmentConfigProps);
  }

  @Bean
  public BeanFactoryPostProcessor beanFactoryPostProcessor(ProfileScope profileScope) {
    return beanFactory -> beanFactory.registerScope("profile", profileScope);
  }
}
