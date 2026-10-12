package org.molgenis.connection.flower;

import java.io.InputStream;
import java.util.function.Consumer;
import org.apache.hc.core5.http.NotImplementedException;
import org.molgenis.connection.ServerConnection;
import org.molgenis.connection.ServerResult;
import org.springframework.stereotype.Component;

@Component
public class FlowerServerConnection implements ServerConnection {
  FlowerContainerConnection flowerContainerConnection;

  public FlowerServerConnection(FlowerContainerConnection flowerContainerConnection) {
    this.flowerContainerConnection = flowerContainerConnection;
  }

  @Override
  public ServerResult eval(String expr) throws Exception {
    throw new NotImplementedException("Method not implemented");
  }


  @Override
  public void writeFile(String fileName, InputStream in) throws Exception {
   flowerContainerConnection.pushData(fileName, in);
  }

  @Override
  public void readFile(String fileName, Consumer<InputStream> inputStreamConsumer)
      throws Exception {
    throw new NotImplementedException("Method not implemented");
  }

  @Override
  public boolean close() {
    return false;
  }
}
