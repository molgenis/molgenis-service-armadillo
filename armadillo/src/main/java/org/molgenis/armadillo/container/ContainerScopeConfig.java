package org.molgenis.armadillo.container;

import static java.lang.String.format;
import static org.molgenis.armadillo.container.ActiveContainerNameAccessor.getActiveContainerName;
import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import org.molgenis.armadillo.exceptions.UnknownContainerException;
import org.molgenis.armadillo.exceptions.UnsupportedContainerTypeException;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.r.RConnectionFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContainerScopeConfig {

  private ContainerConfig getActiveContainerConfig(ContainerService containerService) {
    var activeContainerName = getActiveContainerName();
    try {
      return runAsSystem(() -> containerService.getByName(activeContainerName));
    } catch (UnknownContainerException e) {
      throw new IllegalStateException(
          format("Missing container configuration for active container '%s'.", activeContainerName),
          e);
    }
  }

  @Bean
  @org.molgenis.armadillo.container.annotation.ContainerScope
  public ContainerConfig containerConfig(ContainerService containerService) {
    return getActiveContainerConfig(containerService);
  }

  @Bean
  @org.molgenis.armadillo.container.annotation.ContainerScope
  public DatashieldContainerConfig datashieldContainerConfig(ContainerService containerService) {
    var containerConfig = getActiveContainerConfig(containerService);
    if (containerConfig instanceof DatashieldContainerConfig datashieldConfig) {
      return datashieldConfig;
    }
    throw new UnsupportedContainerTypeException(
        format(
            "Container type '%s' does not support DataSHIELD features.",
            containerConfig.getClass().getSimpleName()));
  }

  @Bean
  @org.molgenis.armadillo.container.annotation.ContainerScope
  public RConnectionFactory rConnectionFactory(
      DatashieldContainerConfig datashieldConfig,
      DatashieldRConnectionFactoryProvider datashieldProvider) {
    return datashieldProvider.create(datashieldConfig);
  }

  @Bean
  public static BeanFactoryPostProcessor beanFactoryPostProcessor(ContainerScope containerScope) {
    return beanFactory -> beanFactory.registerScope("container", containerScope);
  }
}
