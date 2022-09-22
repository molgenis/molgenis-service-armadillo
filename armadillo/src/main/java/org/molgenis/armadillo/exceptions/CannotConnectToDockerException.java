package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.molgenis.armadillo.metadata.ProfileConfig;

public class CannotConnectToDockerException extends RuntimeException {
  public CannotConnectToDockerException(ProfileConfig profileConfig, Exception e) {
    super(
        format(
            "Cannot get docker state for profile '%s'. Error: %s",
            profileConfig.getName(), e.getMessage()));
  }
}
