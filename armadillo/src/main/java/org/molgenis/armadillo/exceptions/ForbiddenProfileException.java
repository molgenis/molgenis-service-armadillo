package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.FORBIDDEN)
public class ForbiddenProfileException extends RuntimeException {

  public ForbiddenProfileException(String profileName, Set<String> allowedProfiles) {
    super(
        format(
            "You are not allowed to use profile '%s'. Allowed profiles are %s",
            profileName, allowedProfiles));
  }
}
