package org.molgenis.datashield.exceptions;

import static java.lang.String.format;

public class IllegalRPackageException extends RuntimeException {

  public IllegalRPackageException(String rPackageName) {
    super(format("R package '%s' is not whitelisted", rPackageName));
  }
}
