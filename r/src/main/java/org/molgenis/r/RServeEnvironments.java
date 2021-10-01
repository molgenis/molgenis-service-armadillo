package org.molgenis.r;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Map;
import org.molgenis.r.config.RServeConfig;
import org.springframework.stereotype.Component;

@Component
public class RServeEnvironments {
  // TODO: validate the config!
  private final Map<String, RConnectionFactory> connectionFactories;

  public RServeEnvironments(RServeConfig rServeConfig) {
    connectionFactories =
        rServeConfig.getEnvironments().stream()
            .map(RConnectionFactoryImpl::new)
            .collect(toUnmodifiableMap(RConnectionFactoryImpl::getName, x -> x));
  }

  public RConnectionFactory getConnectionFactory(String environment) {
    return connectionFactories.get(environment);
  }
}
