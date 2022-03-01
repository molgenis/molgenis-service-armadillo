package org.molgenis.r.exceptions;

import static java.lang.String.format;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FailedRPackageInstallException extends RuntimeException {
  public FailedRPackageInstallException(String packageName) {
    super(
        format(
            "The package [ '%s' ] is not installed correctly. It could be a dependency problem.",
            packageName));
  }
}
