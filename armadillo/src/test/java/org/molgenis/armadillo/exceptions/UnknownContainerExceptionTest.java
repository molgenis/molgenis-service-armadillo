package org.molgenis.armadillo.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnknownContainerExceptionTest {

  @Test
  void constructor_formatsMessage() {
    UnknownContainerException ex = new UnknownContainerException("missing");

    assertEquals("Container: missing not found", ex.getMessage());
  }
}
