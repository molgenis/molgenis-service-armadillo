package org.molgenis.armadillo.container;

import static java.lang.String.format;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DefaultContainerFactoryConfig {

  private final String defaultType;

  public DefaultContainerFactoryConfig(
      @Value("${armadillo.container.defaults.type:ds}") String defaultType) {
    this.defaultType = defaultType;
  }

  @Bean
  @Primary
  public DefaultContainerFactory defaultContainerFactory(List<DefaultContainerFactory> factories) {
    return factories.stream()
        .filter(factory -> defaultType.equalsIgnoreCase(factory.getType()))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    format("No DefaultContainerFactory registered for type '%s'.", defaultType)));
  }
}
