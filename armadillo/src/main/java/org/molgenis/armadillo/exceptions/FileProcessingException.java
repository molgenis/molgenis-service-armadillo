package org.molgenis.armadillo.exceptions;

public class FileProcessingException extends RuntimeException {

  public FileProcessingException() {
    super("Error processing file");
  }
}
