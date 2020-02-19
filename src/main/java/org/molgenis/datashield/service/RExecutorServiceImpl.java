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
public class RExecutorServiceImpl implements RExecutorService {

  private static final String R_PACKAGE_REPO_URL = "http://cran.r-project.org";

  @Override
  public String execute(String cmd, RConnection connection)
      throws RserveException, REXPMismatchException {
    REXP value = connection.eval(cmd);
    return value.asString();
  }

  @Override
  public String assign(InputStream csv, Table table, RConnection connection)
      throws IOException, RserveException {
    String dataFileName = table.name() + ".csv";
    copyFile(csv, dataFileName, connection);
    ensurePackage("readr", connection);
    return assignTable(table, dataFileName, connection);
  }

  private void copyFile(InputStream csv, String dataFileName, RConnection connection)
      throws IOException {
    RFileOutputStream outputStream = connection.createFile(dataFileName);
    IOUtils.copy(csv, outputStream);
    csv.close();
    outputStream.close();
  }

  /**
   * Ensure that a R package is installed: check it is available, if not install it.
   *
   * @param packageName the name of the R package
   * @throws RserveException when installation fails
   */
  @SuppressWarnings("SameParameterValue")
  private void ensurePackage(String packageName, RConnection connection) throws RserveException {
    String cmd =
        String.format(
            "if (!require(%s)) { install.packages('%s', repos=c('%s'), dependencies=TRUE) }",
            packageName, packageName, R_PACKAGE_REPO_URL);
    connection.eval(cmd);
  }

  private String assignTable(Table table, String dataFileName, RConnection connection)
      throws RserveException {
    REXP rexp =
        connection.eval(
            String.format(
                "base::is.null(base::assign('%s', readr::read_csv('%s')))",
                table.name(), dataFileName));

    connection.eval(String.format("base::unlink('%s')", dataFileName));

    return rexp.toString();
  }
}
