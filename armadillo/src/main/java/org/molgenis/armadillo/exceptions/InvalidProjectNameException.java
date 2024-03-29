package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class InvalidProjectNameException extends RuntimeException {

  public InvalidProjectNameException(String projectName) {
    super(
        format(
            "Project name '%s' is invalid. Names should adhere to the rules described at "
                + "https://docs.aws.amazon.com/AmazonS3/latest/userguide/bucketnamingrules.html",
            projectName));
  }
}
