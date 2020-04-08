package org.molgenis.datashield.exceptions;

import static java.lang.String.format;

public class IllegalRPackageException extends RuntimeException {

  public IllegalRPackageException(String function, String package_) {
    super(
        format(
            "Error while registering function '%s': package '%s' is not whitelisted",
            function, package_));
  }
}
