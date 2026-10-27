package org.molgenis.armadillo.controller;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.r.exceptions.ConnectionCreationFailedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ControllerExceptionHandler {

  @ExceptionHandler(StorageException.class)
  protected ResponseEntity<String> handleStorageException() {
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
        .body("Something went wrong while reading/writing in the storage");
  }

  @ExceptionHandler(ConnectionCreationFailedException.class)
  protected ResponseEntity<String> handleConnectionCreationFailed() {
    return ResponseEntity.status(SERVICE_UNAVAILABLE)
        .body(
            "Could not connect to the profile's R server. "
                + "The profile may exist but its container is not started or not reachable.");
  }
}
