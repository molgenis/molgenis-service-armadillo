package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SuperExecEntrypointMissingException extends RuntimeException {
  public SuperExecEntrypointMissingException(String image, String entrypoint, Throwable cause) {
    super(
        format(
            "Image '%s' cannot be used as a superexec: it does not contain the '%s' entrypoint",
            image, entrypoint),
        cause);
  }
}
