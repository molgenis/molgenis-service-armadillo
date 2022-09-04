package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.molgenis.armadillo.metadata.ProfileDetails;

public class CannotConnectToDockerException extends RuntimeException {
  public CannotConnectToDockerException(ProfileDetails profileDetails, Exception e) {
    super(
        format(
            "Cannot get docker state for profile '%s'. Error: %s",
            profileDetails.getName(), e.getMessage()));
  }
}
