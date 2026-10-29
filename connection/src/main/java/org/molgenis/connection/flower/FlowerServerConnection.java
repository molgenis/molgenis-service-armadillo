package org.molgenis.connection.flower;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import org.apache.hc.core5.http.NotImplementedException;
import org.molgenis.connection.ServerConnection;
import org.molgenis.connection.ServerResult;
import org.molgenis.connection.flower.helpers.ArmadilloDataFile;

public class FlowerServerConnection implements ServerConnection {
  /***
   * Flower equivalent for RockConnection and RServeConnection
   * */
  FlowerContainerConnection flowerContainerConnection;

  public FlowerServerConnection(FlowerContainerConnection flowerContainerConnection) {
    this.flowerContainerConnection = flowerContainerConnection;
  }

  @Override
  public ServerResult eval(String expr) throws Exception {
    throw new NotImplementedException("Method not implemented");
  }

  @Override
  public void writeFile(String filePath, InputStream in) {
    ArmadilloDataFile armadilloDataFile = new ArmadilloDataFile(filePath);
    // url encode so that "%2F" stands in for "/", instead of "_" so "data/train" and "data_train"
    // can't collide
    String urlEncodedDataFileName =
        URLEncoder.encode(armadilloDataFile.getArmadilloFilePath(), StandardCharsets.UTF_8);
    long size = armadilloDataFile.getSize();
    flowerContainerConnection.pushData(urlEncodedDataFileName, in, size);
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
