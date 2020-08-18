package org.molgenis.r.service;

import java.util.List;
import org.molgenis.r.model.RProcess;
import org.rosuda.REngine.Rserve.RConnection;

public interface ProcessService {

  int countRserveProcesses(RConnection connection);

  List<RProcess> getRserveProcesses(RConnection connection);
}
