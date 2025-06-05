package org.molgenis.armadillo.exceptions;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(BAD_REQUEST)
public class FileProcessingException extends RuntimeException {

  public FileProcessingException() {
    super("Error processing file");
  }

  public FileProcessingException(String message) {
    super(message);
  }
}
