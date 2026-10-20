package org.molgenis.connection.flower.service;

import java.io.InputStream;
import org.molgenis.connection.flower.FlowerContainerConnection;
import org.molgenis.connection.flower.FlowerServerConnection;
import org.springframework.stereotype.Component;

@Component
public class FlowerExecutorService {
  public FlowerExecutorService() {}

  private FlowerContainerConnection connect(String container) {
    return new FlowerContainerConnection(container);
  }

  // connection between armadillo and connection part of application for flower
  public void loadData(String container, String fileName, InputStream data) throws Exception {
    FlowerServerConnection serverConnection = new FlowerServerConnection(this.connect(container));
    serverConnection.writeFile(fileName, data);
  }
}
