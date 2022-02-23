package org.molgenis.armadillo.info;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.molgenis.r.RConnectionFactoryImpl;
import org.molgenis.r.config.EnvironmentConfigProps;
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
    // TODO: make this available in the /actuator/ endpoint
    return rServeConfig.getEnvironments().stream()
        .map(EnvironmentConfigProps::getName)
        .map(
            environmentName ->
                REnvironment.create(
                    environmentName,
                    doWithConnection(environmentName, processService::getRserveProcesses)))
        .collect(Collectors.toList());
  }

  <T> T doWithConnection(String environmentName, Function<RConnection, T> action) {
    var environment =
        rServeConfig.getEnvironments().stream()
            .filter(it -> environmentName.equals(it.getName()))
            .findFirst()
            .orElseThrow();
    RConnection connection = connect(environment);
    try {
      return action.apply(connection);
    } finally {
      connection.close();
    }
  }

  RConnection connect(EnvironmentConfigProps environment) {
    return new RConnectionFactoryImpl(environment).tryCreateConnection();
  }

  @DeleteOperation
  public void deleteRServeProcess(String environmentName, int pid) {
    // TODO: make this available in the /actuator/ endpoint
    doWithConnection(
        environmentName,
        connection -> {
          processService.terminateProcess(connection, pid);
          return null;
        });
  }

  public int countRServeProcesses(String environmentName) {
    return doWithConnection(environmentName, processService::countRserveProcesses);
  }
}
