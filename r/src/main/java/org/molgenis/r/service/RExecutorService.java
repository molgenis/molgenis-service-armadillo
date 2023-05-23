package org.molgenis.r.service;

import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import java.util.function.Consumer;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerResult;
import org.springframework.core.io.Resource;

public interface RExecutorService {
  RServerResult execute(String cmd, RServerConnection connection);

  void saveWorkspace(RServerConnection connection, Consumer<InputStream> inputStreamConsumer);

  void loadWorkspace(RServerConnection connection, Resource resource, String environment);

  void loadTable(
      RServerConnection connection,
      Resource resource,
      String filename,
      String symbol,
      List<String> variables);

  void loadResource(
      RServerConnection connection, Resource resource, String filename, String symbol);

  void loadResource(
      Principal principal,
      RServerConnection connection,
      Resource resource,
      String filename,
      String symbol);

  void installPackage(RServerConnection connection, Resource packageResource, String name);
}
