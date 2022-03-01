package org.molgenis.r.exceptions;

import static java.lang.String.format;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRPackageException extends RuntimeException {
  public InvalidRPackageException(String packageName) {
    super(
        format(
            "The file [ '%s' ] must be a (binary) package built from R sources and have the "
                + "extension '.tar.gz' or '.tgz'",
            packageName));
  }
}
