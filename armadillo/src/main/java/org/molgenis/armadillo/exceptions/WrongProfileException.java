package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN)
public class WrongProfileException extends RuntimeException {

  public WrongProfileException(
      String projectName, String profileName, Set<String> allowedProfiles) {
    super(
        format(
            "It is not allowed to load data from project '%s' in profile '%s'. Allowed"
                + " profiles for this project are: %s",
            projectName, profileName, allowedProfiles));
  }
}
