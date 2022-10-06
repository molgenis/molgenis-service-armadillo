package org.molgenis.armadillo.metadata;

public enum ProfileStatus {
  RUNNING,
  STOPPED,
  NOT_FOUND,
  DOCKER_OFFLINE;

  public static ProfileStatus ofDockerStatus(String status) {
    return switch (status) {
      case "running" -> ProfileStatus.RUNNING;
      case "exited" -> ProfileStatus.STOPPED;
      default -> throw new IllegalStateException("Unsupported container status");
    };
  }
}
