package org.molgenis.r.service;

import java.io.InputStream;
import java.util.function.Consumer;
import org.molgenis.r.model.Table;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.Rserve.RConnection;
import org.springframework.core.io.Resource;

public interface RExecutorService {
  REXP execute(String cmd, RConnection connection);

  String assign(Resource resource, Table table, RConnection connection);

  void saveWorkspace(RConnection connection, Consumer<InputStream> inputStreamConsumer);

  void loadWorkspace(RConnection connection, Resource resource);
}
