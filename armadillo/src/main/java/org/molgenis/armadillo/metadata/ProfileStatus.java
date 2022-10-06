package org.molgenis.armadillo.metadata;

import static java.lang.Boolean.TRUE;

import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;

public enum ProfileStatus {
  RUNNING,
  NOT_RUNNING,
  NOT_FOUND,
  DOCKER_OFFLINE;

  public static ProfileStatus ofDockerStatus(ContainerState state) {
    return TRUE.equals(state.getRunning()) ? RUNNING : NOT_RUNNING;
  }

  public static ProfileStatus ofDockerStatus(String status) {
    return status.equals("running") ? RUNNING : NOT_RUNNING;
  }
}
