package org.molgenis.armadillo.controller;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import org.molgenis.armadillo.exceptions.StorageException;
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
}
