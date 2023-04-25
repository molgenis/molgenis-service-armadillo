package org.molgenis.r.rserve;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerException;
import org.molgenis.r.RServerResult;
import org.molgenis.r.exceptions.RExecutionException;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;

/** Direct connection with Rserve, through its java API. */
public class RserveConnection implements RServerConnection {

  private final RConnection connection;

  public RserveConnection(RConnection connection) {
    this.connection = connection;
  }

  @Override
  public RServerResult eval(String expr) throws RServerException {
    return new RserveResult(evalREXP(expr));
  }

  @Override
  public OutputStream createFile(String fileName) throws IOException {
    return connection.createFile(fileName);
  }

  @Override
  public InputStream openFile(String fileName) throws IOException {
    return connection.openFile(fileName);
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
