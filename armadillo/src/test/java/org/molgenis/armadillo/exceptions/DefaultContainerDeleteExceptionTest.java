package org.molgenis.armadillo.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DefaultContainerDeleteExceptionTest {

  @Test
  void constructor_setsMessage() {
    DefaultContainerDeleteException ex = new DefaultContainerDeleteException();

    assertEquals("The default container can't be deleted", ex.getMessage());
  }
}
