package org.molgenis.connection.flower;

import java.io.File;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.apache.hc.core5.http.NotImplementedException;
import org.molgenis.connection.ServerConnection;
import org.molgenis.connection.ServerResult;

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
    String[] splittedPath = fileName.split("/");
    String project = splittedPath[splittedPath.length - 3];
    String folder = splittedPath[splittedPath.length - 2];
    String file = splittedPath[splittedPath.length - 1];
    String armadilloFile =
        URLEncoder.encode(String.format("%s/%s/%s", project, folder, file), StandardCharsets.UTF_8);
    long size = new File(fileName).length();
    flowerContainerConnection.pushData(armadilloFile, in, size);
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
