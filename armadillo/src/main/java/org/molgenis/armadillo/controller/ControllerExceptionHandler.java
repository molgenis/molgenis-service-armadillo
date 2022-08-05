package org.molgenis.armadillo.controller;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.molgenis.armadillo.exceptions.DuplicateProjectException;
import org.molgenis.armadillo.exceptions.UnknownProjectException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ControllerExceptionHandler {

  @ExceptionHandler(DuplicateProjectException.class)
  protected ResponseEntity<String> handleDuplicateProject(DuplicateProjectException ex) {
    return ResponseEntity.status(CONFLICT).body(ex.getMessage());
  }

  @ExceptionHandler(UnknownProjectException.class)
  protected ResponseEntity<String> handleUnknownProject(UnknownProjectException ex) {
    return ResponseEntity.status(NOT_FOUND).body(ex.getMessage());
  }
}
