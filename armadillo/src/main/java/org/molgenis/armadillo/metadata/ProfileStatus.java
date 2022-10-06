package org.molgenis.armadillo.metadata;

import static java.lang.Boolean.TRUE;

import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;

public enum ProfileStatus {
  RUNNING,
  NOT_RUNNING,
  NOT_FOUND,
  DOCKER_OFFLINE;

  public static ProfileStatus ofDockerStatus(ContainerState state) {
    if (TRUE.equals(state.getRunning())) {
      return RUNNING;
    } else {
      return NOT_RUNNING;
    }
  }

  public static ProfileStatus ofDockerStatus(String status) {
    if (status.equals("running")) {
      return RUNNING;
    } else {
      return NOT_RUNNING;
    }
  }
}
