package org.molgenis.armadillo.info;


import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.r.RServeEnvironments;
import org.molgenis.r.config.RServeConfig;
import org.molgenis.r.model.REnvironment;
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
  private final RServeEnvironments rServeEnvironments;
  private final RServeConfig rServeConfig;

  public RProcessEndpoint(ProcessService processService, RServeConfig rServeConfig, RServeEnvironments rServeEnvironments) {
    this.processService = processService;
    this.rServeConfig = rServeConfig;
    this.rServeEnvironments = rServeEnvironments;
  }

  @ReadOperation
  public List<REnvironment> getRServeEnvironments() {
    return rServeConfig.getEnvironments().stream().map(environment -> {
      var name = environment.getName();
      var rConnectionFactory = rServeEnvironments.getConnectionFactory(name);
      final RConnection connection = rConnectionFactory.createConnection();
      try {
        var processes = processService.getRserveProcesses(connection);
        return REnvironment.create(name, processes);
      } finally {
        connection.close();
      }
    }).collect(Collectors.toList());
  }

  @DeleteOperation
  public void deleteRServeProcess(String environment, int pid) {
    var rConnectionFactory = rServeEnvironments.getConnectionFactory(environment);
    final RConnection connection = rConnectionFactory.createConnection();
    try {
      processService.terminateProcess(connection, pid);
    } finally {
      connection.close();
    }
  }

  public int countRServeProcesses(String environment) {
    var rConnectionFactory = rServeEnvironments.getConnectionFactory(environment);
    final RConnection connection = rConnectionFactory.createConnection();
    try {
      return processService.countRserveProcesses(connection);
    } finally {
      connection.close();
    }
  }
}
