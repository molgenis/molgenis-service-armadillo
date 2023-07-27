package org.molgenis.armadillo.info;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.molgenis.armadillo.metadata.ProfileConfig;
import org.molgenis.armadillo.metadata.ProfileService;
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
  private final ProfileService profileService;

  public RProcessEndpoint(ProcessService processService, ProfileService profileService) {
    this.processService = processService;
    this.profileService = profileService;
  }

  @ReadOperation
  public List<REnvironment> getRServeEnvironments() {
    // TODO: make this available in the /actuator/ endpoint
    return profileService.getAll().stream()
        .map(ProfileConfig::getName)
        .map(
            environmentName ->
                REnvironment.create(
                    environmentName,
                    doWithConnection(environmentName, processService::getRserveProcesses)))
        .collect(Collectors.toList());
  }

  <T> T doWithConnection(String environmentName, Function<RServerConnection, T> action) {
    var environment =
        profileService.getAll().stream()
            .filter(it -> environmentName.equals(it.getName()))
            .map(ProfileConfig::toEnvironmentConfigProps)
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
