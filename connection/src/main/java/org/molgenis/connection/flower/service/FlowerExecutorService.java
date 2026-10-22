package org.molgenis.connection.flower.service;

import java.io.InputStream;
// import org.molgenis.connection.flower.FlowerContainerConnection;
// import org.molgenis.connection.flower.FlowerServerConnection;
import org.molgenis.connection.flower.FlowerContainerConnection;
import org.molgenis.connection.flower.FlowerServerConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FlowerExecutorService {
  private final FlowerDockerService dockerService;

  public FlowerExecutorService(@Autowired FlowerDockerService flowerDockerService) {
    dockerService = flowerDockerService;
  }

  private FlowerContainerConnection connect(String container) {
    return new FlowerContainerConnection(container, dockerService);
  }

  // connection between armadillo and connection part of application for flower
  // can we use fully qualified filename so we can get filesize?
  public void loadData(String container, String fileName, InputStream data) throws Exception {
    FlowerContainerConnection connection = this.connect(container);
    FlowerServerConnection serverConnection = new FlowerServerConnection(connection);
    serverConnection.writeFile(fileName, data);
  }
}
