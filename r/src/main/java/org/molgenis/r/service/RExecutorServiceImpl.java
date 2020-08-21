package org.molgenis.r.service;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;
import static org.molgenis.r.Formatter.quote;

import com.google.common.base.Stopwatch;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.molgenis.r.Formatter;
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
  public static final int RFILE_BUFFER_SIZE = 65536;

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

  @Override
  public void loadTable(
      RConnection connection, Resource resource, String filename, String symbol, String variables) {
    LOGGER.debug("Load table from file {} into {}", filename, symbol);
    String rFileName = filename.replace("/", "_");
    try {
      copyFile(resource, rFileName, connection);
      if (filename.endsWith(".parquet")) {
        if (variables == null) {
          connection.eval(
              format(
                  "is.null(base::assign('%s', value={arrow::read_parquet('%s')}))",
                  symbol, rFileName));
        } else {
          var colSelect =
              Formatter.stringVector(
                  Arrays.stream(variables.split(","))
                      .map(String::trim)
                      .collect(toList())
                      .toArray(new String[] {}));
          connection.eval(
              format(
                  "is.null(base::assign('%s', value={arrow::read_parquet('%s', col_select = %s)}))",
                  symbol, rFileName, colSelect));
        }
      } else {
        // TODO: .rda?
      }
      connection.eval(format("base::unlink('%s')", rFileName));
    } catch (IOException | RserveException e) {
      throw new RExecutionException(e);
    }
  }

  void copyFile(Resource resource, String dataFileName, RConnection connection) throws IOException {
    LOGGER.info("Copying '{}' to R...", dataFileName);
    Stopwatch sw = Stopwatch.createStarted();
    try (InputStream is = resource.getInputStream();
        RFileOutputStream os = connection.createFile(dataFileName);
        BufferedOutputStream bos = new BufferedOutputStream(os, RFILE_BUFFER_SIZE)) {
      long size = IOUtils.copyLarge(is, bos);
      if (LOGGER.isDebugEnabled()) {
        var elapsed = sw.elapsed(TimeUnit.MICROSECONDS);
        LOGGER.debug(
            "Copied {} in {}ms [{} MB/s]",
            byteCountToDisplaySize(size),
            elapsed / 1000,
            format("%.03f", size * 1.0 / elapsed));
      }
    }
  }
}
