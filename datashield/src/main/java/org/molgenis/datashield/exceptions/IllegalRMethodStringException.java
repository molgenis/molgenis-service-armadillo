package org.molgenis.datashield.exceptions;

public class IllegalRMethodStringException extends RuntimeException {

  public IllegalRMethodStringException(String methodString) {
    super(methodString);
  }
}
