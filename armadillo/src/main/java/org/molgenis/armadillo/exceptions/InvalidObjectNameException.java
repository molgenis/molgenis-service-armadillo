package org.molgenis.armadillo.exceptions;

import static java.lang.String.format;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class InvalidObjectNameException extends RuntimeException {

    public InvalidObjectNameException(String objectName) {
        super(
                format(
                        "Object name '%s' is invalid. Object format should be in the following format: folder/file.",
                        objectName));
    }
}
