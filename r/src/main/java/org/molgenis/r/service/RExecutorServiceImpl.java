package org.molgenis.r.service;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.molgenis.r.model.Column;
import org.molgenis.r.model.ColumnType;
import org.molgenis.r.model.Table;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.rosuda.REngine.Rserve.RFileOutputStream;
import org.rosuda.REngine.Rserve.RserveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RExecutorServiceImpl implements RExecutorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(RExecutorServiceImpl.class);

  private static final String R_PACKAGE_REPO_URL = "http://cran.r-project.org";

  @Override
  public REXP execute(String cmd, RConnection connection) throws RserveException {
    return connection.eval(cmd);
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
        format(
            "if (!require(%s)) { install.packages('%s', repos=c('%s'), dependencies=TRUE) }",
            packageName, packageName, R_PACKAGE_REPO_URL);
    connection.eval(cmd);
  }

  private String assignTable(Table table, String dataFileName, RConnection connection)
      throws RserveException {
    String colTypes = getColTypes(table);
    String command =
        String.format(
            "base::is.null(base::assign('%s', readr::read_csv('%s', col_types = %s, na = c(''))))",
            table.name(), dataFileName, colTypes);
    LOGGER.debug("Executing: {}", command);
    REXP rexp = connection.eval(command);
    connection.eval(format("base::unlink('%s')", dataFileName));

    return rexp.toString();
  }

  public static String getColTypes(Table table) {
    return String.format(
        "cols ( %s )",
        table.columns().stream()
            .map(RExecutorServiceImpl::getColNameAndType)
            .collect(joining(", ")));
  }

  private static String getColNameAndType(Column column) {
    return column.name() + " = " + getColType(column.type());
  }

  public static String getColType(ColumnType type) {
    switch (type) {
      case LOGICAL:
        return "col_logical()";
      case INTEGER:
        return "col_integer()";
      case CHARACTER:
        return "col_character()";
      case DATE:
        return "col_date()";
      case DATE_TIME:
        return "col_datetime()";
      case DOUBLE:
        return "col_double()";
      case FACTOR:
        throw new IllegalArgumentException("Factor columns not yet supported");
      default:
        throw new EnumConstantNotPresentException(ColumnType.class, type.name());
    }
  }
}
