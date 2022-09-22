package org.molgenis.armadillo.metadata;

public enum ProfileStatus {
  RUNNING,
  STOPPED,
  DOCKER_MANAGEMENT_DISABLED, // when docker has been disabled
  CONNECTION_REFUSED, // when enabled, but can't connect
}
