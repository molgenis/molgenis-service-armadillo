package org.molgenis.armadillo.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UnsupportedContainerTypeExceptionTest {

  @Test
  void constructor_setsMessage() {
    UnsupportedContainerTypeException ex = new UnsupportedContainerTypeException("bad type");

    assertEquals("bad type", ex.getMessage());
  }
}
