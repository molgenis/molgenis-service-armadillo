package org.molgenis.r.service;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.molgenis.r.Formatter.quote;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
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
      REXP result = connection.eval(format("try({%s})", cmd));
      if (result.inherits("try-error")) {
        throw new RExecutionException(
            stream(result.asStrings()).map(String::trim).collect(joining("; ")));
      }
      return result;
    } catch (RserveException | REXPMismatchException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void saveWorkspace(
      String inclusionPattern, RConnection connection, Consumer<InputStream> inputStreamConsumer) {
    try {
      LOGGER.debug("Save workspace");
      String command =
          format(
              "base::save(list = base::grep(%s, base::ls(all.names=T), perl=T, value=T), file=\".RData\")",
              quote(inclusionPattern));
      execute(command, connection);
      try (RFileInputStream is = connection.openFile(".RData")) {
        inputStreamConsumer.accept(is);
      }
    } catch (IOException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void loadWorkspace(RConnection connection, Resource resource, String environment) {
    LOGGER.debug("Load workspace into {}", environment);
    try {
      copyFile(resource, ".RData", connection);
      connection.eval(format("base::load(file='.RData', envir=%s)", environment));
      connection.eval("base::unlink('.RData')");
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
