package org.molgenis.r.rserve;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.io.FileUtils.byteCountToDisplaySize;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.RServerResult;
import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Direct connection with Rserve, through its java API. */
public class RserveConnection implements RServerConnection {

  public static final int RFILE_BUFFER_SIZE = 65536;

  private static final Logger logger = LoggerFactory.getLogger(RserveConnection.class);

  private final RConnection connection;

  public RserveConnection(RConnection connection) {
    this.connection = connection;
  }

  @Override
  public RServerResult eval(String expr, boolean serialized) throws RServerException {
    if (serialized)
      return new RserveResult(evalREXP(format("try(base::serialize({%s}, NULL))", expr)));
    else return new RserveResult(evalREXP(format("try({%s})", expr)));
  }

  @Override
  public void writeFile(String fileName, InputStream in) throws RServerException {
    Stopwatch sw = Stopwatch.createStarted();
    try (OutputStream os = connection.createFile(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(os, RFILE_BUFFER_SIZE)) {
      long size = IOUtils.copyLarge(in, bos);
      if (logger.isDebugEnabled()) {
        var elapsed = sw.elapsed(TimeUnit.MICROSECONDS);
        logger.debug(
            "Copied {} in {}ms [{} MB/s]",
            byteCountToDisplaySize(size),
            elapsed / 1000,
            format("%.03f", size * 1.0 / elapsed));
      }
    } catch (IOException e) {
      throw new RserveException(e);
    }
  }

  @Override
  public void readFile(String fileName, Consumer<InputStream> inputStreamConsumer)
      throws RServerException {
    try (InputStream is = connection.openFile(".RData")) {
      inputStreamConsumer.accept(is);
    } catch (IOException e) {
      throw new RserveException(e);
    }
  }

  @Override
  public boolean close() {
    return connection.close();
  }

  @VisibleForTesting
  public RConnection getConnection() {
    return connection;
  }

  //
  // Private methods
  //

  private REXP evalREXP(String expr) throws RServerException {
    try {
      REXP rexp = connection.eval(expr);
      if (rexp.inherits("try-error")) {
        throw new RExecutionException(
            stream(rexp.asStrings()).map(String::trim).collect(joining("; ")));
      }
      return rexp;
    } catch (org.rosuda.REngine.Rserve.RserveException | REXPMismatchException e) {
      throw new RserveException(e);
    }
  }
}
