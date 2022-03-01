package org.molgenis.r.exceptions;

import static java.lang.String.format;

public class InvalidRPackageException extends RuntimeException {
  public InvalidRPackageException(String packageName) {
    super(
        format(
            "The file [ '%s' ] must be a source package built and have the extension '.tar.gz''",
            packageName));
  }
}
