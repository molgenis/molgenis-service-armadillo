package org.molgenis.datashield.exceptions;

import java.io.IOException;

public class DatashieldRequestFailedException extends RuntimeException {

  public DatashieldRequestFailedException(String message, IOException err) {
    super(message, err);
  }
}
