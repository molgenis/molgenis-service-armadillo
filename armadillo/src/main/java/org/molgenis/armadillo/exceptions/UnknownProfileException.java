package org.molgenis.armadillo.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static java.lang.String.format;

@ResponseStatus(code = HttpStatus.NOT_FOUND)
public class UnknownProfileException extends RuntimeException {

    public UnknownProfileException(String profileName) {
        super(format("Profile: %s not found", profileName));
    }
}
