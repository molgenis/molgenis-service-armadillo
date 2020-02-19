package org.molgenis.datashield.service;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.molgenis.datashield.service.model.Table;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPMismatchException;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.rosuda.REngine.Rserve.RserveException;
import org.springframework.stereotype.Component;

@Component
public class RExecutorServiceImpl {

  public String exec(String cmd, RConnection connection)
      throws RserveException, REXPMismatchException {
    REXP value = connection.eval(cmd);
    return value.asString();
  }

  public String assign(InputStream csv, Table table, RConnection connection)
      throws IOException, RserveException {
    String DATA_FILE_NAME = table.name() + ".csv";

    RFileOutputStream outputStream = connection.createFile(DATA_FILE_NAME);
    IOUtils.copy(csv, outputStream);
    csv.close();
    outputStream.close();

    ensurePackage("readr", connection);
    REXP rexp =
        connection.eval(
            String.format(
                "base::is.null(base::assign('%s', readr::read_csv('%s')))",
                table.name(), DATA_FILE_NAME));

    connection.eval(String.format("base::unlink('%s')", DATA_FILE_NAME));

    return rexp.toString();
  }

  /**
   * Ensure that a R package is installed: check it is available, if not install it.
   *
   * @param packageName
   * @return
   */
  private REXP ensurePackage(String packageName, RConnection connection) throws RserveException {
    String repos = "http://cran.r-project.org";
    String cmd =
        String.format(
            "if (!require(%s)) { install.packages('%s', repos=c('%s'), dependencies=TRUE) }",
            packageName, packageName, repos);
    return connection.eval(cmd);
  }
}
