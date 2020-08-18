package org.molgenis.r.service;

import java.util.List;
import org.molgenis.r.model.RProcess;
import org.rosuda.REngine.Rserve.RConnection;

public interface ProcessService {

  int countRserveProcesses(RConnection connection);

  List<RProcess> getRserveProcesses(RConnection connection);

  int getPid(RConnection connection);

  /**
   * Terminate a process with given pid on the R server.
   *
   * @param connection unrelated connection, used to terminate the process
   * @param pid pid of the process to kill
   */
  void terminateProcess(RConnection connection, int pid);
}
