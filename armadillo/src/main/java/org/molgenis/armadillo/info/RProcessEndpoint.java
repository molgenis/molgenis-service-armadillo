package org.molgenis.armadillo.info;

import java.util.List;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.model.RProcess;
import org.molgenis.r.service.ProcessService;
import org.rosuda.REngine.Rserve.RConnection;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "rserveProcesses")
public class RProcessEndpoint {
  private final ProcessService processService;
  private final RConnectionFactory rConnectionFactory;

  public RProcessEndpoint(ProcessService processService, RConnectionFactory rConnectionFactory) {
    this.processService = processService;
    this.rConnectionFactory = rConnectionFactory;
  }

  @ReadOperation
  public List<RProcess> getRServeProcesses() {
    final RConnection connection = rConnectionFactory.createConnection();
    try {
      return processService.getRserveProcesses(connection);
    } finally {
      connection.close();
    }
  }

  @DeleteOperation
  public void deleteRServeProcess(int pid) {
    final RConnection connection = rConnectionFactory.createConnection();
    try {
      processService.terminateProcess(connection, pid);
    } finally {
      connection.close();
    }
  }

  public int countRServeProcesses() {
    final RConnection connection = rConnectionFactory.createConnection();
    try {
      return processService.countRserveProcesses(connection);
    } finally {
      connection.close();
    }
  }
}
