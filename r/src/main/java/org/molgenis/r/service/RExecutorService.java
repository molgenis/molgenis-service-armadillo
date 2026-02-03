package org.molgenis.r.service;

import java.io.InputStream;
import java.security.Principal;
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

  void loadResource(
      Principal principal,
      RServerConnection connection,
      Resource resource,
      String filename,
      String symbol,
      String resourceToken);

  void installPackage(RServerConnection connection, Resource packageResource, String name);
}
