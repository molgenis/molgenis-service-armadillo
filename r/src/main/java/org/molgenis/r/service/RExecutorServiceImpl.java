package org.molgenis.r.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileInputStream;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class RExecutorServiceImpl implements RExecutorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RExecutorServiceImpl.class);

  @Override
  public REXP execute(String cmd, RConnection connection) {
    try {
      LOGGER.debug("Evaluate {}", cmd);
      return connection.eval(cmd);
    } catch (RserveException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void saveWorkspace(RConnection connection, Consumer<InputStream> inputStreamConsumer) {
    try {
      LOGGER.debug("Save workspace");
      connection.eval("base::save.image()");
      try (RFileInputStream is = connection.openFile(".RData")) {
        inputStreamConsumer.accept(is);
      }
    } catch (RserveException | IOException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void loadWorkspace(RConnection connection, Resource resource) {
    LOGGER.debug("Load workspace");
    try {
      copyFile(resource, ".RData", connection);
      connection.eval("base::load(file='.RData')");
    } catch (IOException | RserveException e) {
      throw new RExecutionException(e);
    }
  }

  private void copyFile(Resource resource, String dataFileName, RConnection connection)
      throws IOException {
    try (InputStream is = resource.getInputStream();
        RFileOutputStream outputStream = connection.createFile(dataFileName)) {
      IOUtils.copyLarge(is, outputStream);
    }
  }
}
