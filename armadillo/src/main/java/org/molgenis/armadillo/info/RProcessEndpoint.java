package org.molgenis.armadillo.info;

import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.r.RConnectionFactory;
import org.molgenis.r.RConnectionFactoryImpl;
import org.molgenis.r.config.RServeConfig;
import org.molgenis.r.model.REnvironment;
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
  private final RServeConfig rServeConfig;

  public RProcessEndpoint(ProcessService processService, RServeConfig rServeConfig) {
    this.processService = processService;
    this.rServeConfig = rServeConfig;
  }

  @ReadOperation
  public List<REnvironment> getRServeEnvironments() {
    return rServeConfig.getEnvironments().stream()
        .map(
            environment -> {
              var name = environment.getName();
              var rConnectionFactory = new RConnectionFactoryImpl(environment);
              final RConnection connection = rConnectionFactory.createConnection();
              try {
                var processes = processService.getRserveProcesses(connection);
                return REnvironment.create(name, processes);
              } finally {
                connection.close();
              }
            })
        .collect(Collectors.toList());
  }

  @DeleteOperation
  public void deleteRServeProcess(String environmentName, int pid) {
    var rConnectionFactory = getConnectionFactory(environmentName);
    final RConnection connection = rConnectionFactory.createConnection();
    try {
      processService.terminateProcess(connection, pid);
    } finally {
      connection.close();
    }
  }

  private RConnectionFactory getConnectionFactory(String environmentName) {
    var environment =
        rServeConfig.getEnvironments().stream()
            .filter(it -> environmentName.equals(it.getName()))
            .findFirst()
            .orElseThrow();
    return new RConnectionFactoryImpl(environment);
  }

  public int countRServeProcesses(String environmentName) {
    var rConnectionFactory = getConnectionFactory(environmentName);
    final RConnection connection = rConnectionFactory.createConnection();
    try {
      return processService.countRserveProcesses(connection);
    } finally {
      connection.close();
    }
  }
}
