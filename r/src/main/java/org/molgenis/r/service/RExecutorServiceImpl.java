package org.molgenis.r.service;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.molgenis.r.exceptions.RExecutionException;
import org.molgenis.r.model.Column;
import org.molgenis.r.model.ColumnType;
import org.molgenis.r.model.Table;
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

  private static final String R_PACKAGE_REPO_URL = "http://cran.r-project.org";

  @Override
  public REXP execute(String cmd, RConnection connection) {
    try {
      return connection.eval(cmd);
    } catch (RserveException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public String assign(Resource resource, String assignSymbol, Table table, RConnection connection) {
    try {
      String dataFileName = table.name() + ".csv";
      copyFile(resource, dataFileName, connection);
      return assignTable(assignSymbol, table.columns(), dataFileName, connection);
    } catch (IOException | RserveException e) {
      throw new RExecutionException(e);
    }
  }

  @Override
  public void saveWorkspace(RConnection connection, Consumer<InputStream> inputStreamConsumer) {
    try {
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

  private String assignTable(String assignSymbol, List<Column> columns, String dataFileName, RConnection connection)
      throws RserveException {
    String colTypes = getColTypes(columns);
    String command =
        String.format(
            "base::is.null(base::assign('%s', read_csv('%s', col_types = %s, na = c(''))))",
            assignSymbol, dataFileName, colTypes);
    LOGGER.debug("Executing: {}", command);
    REXP rexp = connection.eval(command);
    connection.eval(format("base::unlink('%s')", dataFileName));

    return rexp.toString();
  }

  public static String getColTypes(List<Column> columns) {
    return String.format(
        "cols ( %s )",
        columns.stream()
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
