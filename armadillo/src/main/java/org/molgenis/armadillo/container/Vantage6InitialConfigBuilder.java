package org.molgenis.armadillo.container;

import org.molgenis.armadillo.metadata.InitialContainerConfig;
import org.springframework.stereotype.Component;

@Component
public class Vantage6InitialConfigBuilder implements InitialConfigBuilder {

  @Override
  public String getType() {
    return "v6";
  }

  @Override
  public ContainerConfig build(InitialContainerConfig initialConfig) {
    return Vantage6ContainerConfig.create(
        initialConfig.getName(),
        initialConfig.getImage(),
        initialConfig.getHost(),
        initialConfig.getPort(),
        null, // lastImageId
        null, // imageSize
        null, // installDate
        null, // versionId
        null, // creationDate
        null, // serverUrl
        null, // apiKey
        null, // collaborationId
        null, // encryptionKey
        null, // allowedAlgorithms
        null, // allowedAlgorithmStores
        null, // authorizedProjects
        null, // dockerArgs
        null // dockerOptions
        );
  }
}
