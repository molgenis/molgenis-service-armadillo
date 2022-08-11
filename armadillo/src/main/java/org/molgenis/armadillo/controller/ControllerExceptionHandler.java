package org.molgenis.armadillo.controller;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

import org.molgenis.armadillo.exceptions.DuplicateObjectException;
import org.molgenis.armadillo.exceptions.DuplicateProjectException;
import org.molgenis.armadillo.exceptions.FileProcessingException;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.exceptions.UnknownObjectException;
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

  @ExceptionHandler(DuplicateObjectException.class)
  protected ResponseEntity<String> handleDuplicateObject(DuplicateObjectException ex) {
    return ResponseEntity.status(CONFLICT).body(ex.getMessage());
  }

  @ExceptionHandler(UnknownObjectException.class)
  protected ResponseEntity<String> handleUnknownObject(UnknownObjectException ex) {
    return ResponseEntity.status(NOT_FOUND).body(ex.getMessage());
  }

  @ExceptionHandler(StorageException.class)
  protected ResponseEntity<String> handleStorageException() {
    return ResponseEntity.status(INTERNAL_SERVER_ERROR)
        .body("Something went wrong while reading/writing in the storage");
  }

  @ExceptionHandler(FileProcessingException.class)
  protected ResponseEntity<String> handleFileProcessingException(FileProcessingException ex) {
    return ResponseEntity.status(BAD_REQUEST).body(ex.getMessage());
  }
}
