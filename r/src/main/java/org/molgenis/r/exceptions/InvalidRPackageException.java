package org.molgenis.r.exceptions;

import static java.lang.String.format;

public class InvalidRPackageException extends RuntimeException {
  public InvalidRPackageException(String packageName) {
    super(
        format(
            "The package [ '%s' ] must package build from as source-package and has the extension '.tar.gz'",
            packageName));
  }
}
