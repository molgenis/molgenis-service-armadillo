package org.molgenis.armadillo.info;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.molgenis.armadillo.config.DataShieldConfigProps;
import org.molgenis.r.RConnectionFactoryImpl;
import org.molgenis.r.config.EnvironmentConfigProps;
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
  private final DataShieldConfigProps dataShieldConfigProps;

  public RProcessEndpoint(
      ProcessService processService, DataShieldConfigProps dataShieldConfigProps) {
    this.processService = processService;
    this.dataShieldConfigProps = dataShieldConfigProps;
  }

  @ReadOperation
  public List<REnvironment> getRServeEnvironments() {
    // TODO: make this available in the /actuator/ endpoint
    return dataShieldConfigProps.getProfiles().stream()
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
        dataShieldConfigProps.getProfiles().stream()
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
