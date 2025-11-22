package org.molgenis.armadillo.container;

import static java.lang.String.format;
import static org.molgenis.armadillo.container.ActiveContainerNameAccessor.getActiveProfileName;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import org.molgenis.armadillo.exceptions.UnknownContainerException;
import org.molgenis.armadillo.metadata.ContainerConfig;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RServerConnectionFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContainerScopeConfig {

  @Bean
  @org.molgenis.armadillo.container.annotation.ProfileScope
  public ContainerConfig profileConfig(ContainerService containerService) {
    var activeProfileName = getActiveProfileName();
    try {
      return runAsSystem(() -> containerService.getByName(activeProfileName));
    } catch (UnknownContainerException e) {
      throw new IllegalStateException(
          format("Missing container configuration for active container '%s'.", activeProfileName));
    }
  }

  @Bean
  @org.molgenis.armadillo.container.annotation.ProfileScope
  public RConnectionFactory rConnectionFactory(ContainerConfig containerConfig) {
    return new RServerConnectionFactory(containerConfig.toEnvironmentConfigProps());
  }

  @Bean
  public static BeanFactoryPostProcessor beanFactoryPostProcessor(ContainerScope containerScope) {
    return beanFactory -> beanFactory.registerScope("container", containerScope);
  }
}
