package org.molgenis.r.service;

import java.util.List;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.model.RProcess;

public interface ProcessService {

  int countRserveProcesses(RServerConnection connection);

  List<RProcess> getRserveProcesses(RServerConnection connection);

  int getPid(RServerConnection connection);

  /**
   * Terminate a process with given pid on the R server.
   *
   * @param connection unrelated connection, used to terminate the process
   * @param pid pid of the process to kill
   */
  void terminateProcess(RServerConnection connection, int pid);
}
