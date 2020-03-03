package org.molgenis.datashield.exceptions;

import java.io.IOException;

public class DataShieldRequestFailedException extends RuntimeException {

  public DataShieldRequestFailedException(String message, IOException err) {
    super(message, err);
  }
}
