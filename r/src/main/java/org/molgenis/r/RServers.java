package org.molgenis.r;

import static java.util.stream.Collectors.toUnmodifiableMap;

import java.util.Map;
import org.molgenis.r.config.RServersConfig;
import org.springframework.stereotype.Component;

@Component
public class RServers {
  private final Map<String, RConnectionFactory> connectionFactories;

  public RServers(RServersConfig rServeConfig) {
    connectionFactories =
        rServeConfig.getNodes().stream()
            .map(RConnectionFactoryImpl::new)
            .collect(toUnmodifiableMap(RConnectionFactoryImpl::getName, x -> x));
  }

  public RConnectionFactory getConnectionFactory(String profileName) {
    return connectionFactories.get(profileName);
  }
}
