package org.molgenis.armadillo.container;

import static java.lang.String.format;
import static org.molgenis.armadillo.container.ActiveContainerNameAccessor.getActiveContainerName;
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
  @org.molgenis.armadillo.container.annotation.ContainerScope
  public ContainerConfig containerConfig(ContainerService containerService) {
    var activeContainerName = getActiveContainerName();
    try {
      return runAsSystem(() -> containerService.getByName(activeContainerName));
    } catch (UnknownContainerException e) {
      throw new IllegalStateException(
          format(
              "Missing container configuration for active container '%s'.", activeContainerName));
    }
  }

  @Bean
  @org.molgenis.armadillo.container.annotation.ContainerScope
  public RConnectionFactory rConnectionFactory(ContainerConfig containerConfig) {
    return new RServerConnectionFactory(containerConfig.toEnvironmentConfigProps());
  }

  @Bean
  public static BeanFactoryPostProcessor beanFactoryPostProcessor(ContainerScope containerScope) {
    return beanFactory -> beanFactory.registerScope("container", containerScope);
  }
}
