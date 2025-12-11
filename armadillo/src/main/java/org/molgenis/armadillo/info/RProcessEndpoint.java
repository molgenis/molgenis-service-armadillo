package org.molgenis.armadillo.info;

import static org.molgenis.armadillo.security.RunAs.runAsSystem;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.molgenis.armadillo.container.AbstractContainerConfig;
import org.molgenis.armadillo.container.ContainerConfig;
import org.molgenis.armadillo.container.DatashieldContainerConfig;
import org.molgenis.armadillo.metadata.ContainerService;
import org.molgenis.r.RServerConnection;
import org.molgenis.r.RServerConnectionFactory;
import org.molgenis.r.config.EnvironmentConfigProps;
import org.molgenis.r.model.REnvironment;
import org.molgenis.r.service.ProcessService;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "rserveProcesses")
public class RProcessEndpoint {
  private final ProcessService processService;
  private final ContainerService containerService;

  public RProcessEndpoint(ProcessService processService, ContainerService containerService) {
    this.processService = processService;
    this.containerService = containerService;
  }

  @ReadOperation
  public List<REnvironment> getRServeEnvironments() {
    // TODO: make this available in the /actuator/ endpoint
    return containerService.getAll().stream()
        .filter(DatashieldContainerConfig.class::isInstance)
        .map(ContainerConfig::getName)
        .map(
            environmentName ->
                REnvironment.create(
                    environmentName,
                    doWithConnection(environmentName, processService::getRserveProcesses)))
        .collect(Collectors.toList());
  }

  <T> T doWithConnection(String environmentName, Function<RServerConnection, T> action) {
    var environment =
        runAsSystem(containerService::getAll).stream()
            .filter(it -> environmentName.equals(it.getName()))
            .filter(DatashieldContainerConfig.class::isInstance)
            .map(DatashieldContainerConfig.class::cast)
            .map(AbstractContainerConfig::toEnvironmentConfigProps)
            .findFirst()
            .orElseThrow();
    RServerConnection connection = connect(environment);
    try {
      return action.apply(connection);
    } finally {
      connection.close();
    }
  }

  RServerConnection connect(EnvironmentConfigProps environment) {
    return new RServerConnectionFactory(environment).tryCreateConnection();
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
