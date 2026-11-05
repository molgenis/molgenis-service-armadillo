package org.molgenis.connection.datashield.exceptions;

import static java.lang.String.format;

public class FailedRPackageInstallException extends RuntimeException {
  public FailedRPackageInstallException(String packageName) {
    super(
        format(
            "The package [ '%s' ] is not installed correctly. It could be a dependency problem.",
            packageName));
  }
}
