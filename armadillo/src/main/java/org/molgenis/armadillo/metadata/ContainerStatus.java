package org.molgenis.armadillo.metadata;

import static java.lang.Boolean.TRUE;

import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;

public enum ContainerStatus {
  RUNNING,
  NOT_RUNNING,
  NOT_FOUND,
  DOCKER_OFFLINE;

  public static ContainerStatus of(ContainerState state) {
    return TRUE.equals(state.getRunning()) ? RUNNING : NOT_RUNNING;
  }

  public static ContainerStatus of(String status) {
    return status.equals("running") ? RUNNING : NOT_RUNNING;
  }
}
