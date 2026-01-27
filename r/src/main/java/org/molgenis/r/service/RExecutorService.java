package org.molgenis.r.service;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerResult;
import org.springframework.core.io.Resource;

public interface RExecutorService {
  default RServerResult execute(String cmd, RServerConnection connection) {
    return execute(cmd, false, connection);
  }

  RServerResult execute(String cmd, boolean serialized, RServerConnection connection);

  void saveWorkspace(RServerConnection connection, Consumer<InputStream> inputStreamConsumer);

  void loadWorkspace(RServerConnection connection, Resource resource, String environment);

  void loadTable(
      RServerConnection connection,
      Resource resource,
      String filename,
      String symbol,
      List<String> variables);

  /**
   * Load a resource into the R session.
   *
   * @param resourceToken a temporary bearer token for accessing the resource file
   * @param connection the R server connection
   * @param resource the resource metadata (RDS file)
   * @param filename the resource filename
   * @param symbol the R symbol to assign the resource to
   */
  void loadResource(
      String resourceToken,
      RServerConnection connection,
      Resource resource,
      String filename,
      String symbol);

  void installPackage(RServerConnection connection, Resource packageResource, String name);
}
