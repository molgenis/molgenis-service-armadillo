package org.molgenis.armadillo.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

class ContainerRemoveFailedExceptionTest {

  @Test
  void constructor_setsMessageAndCause() {
    Exception cause = new Exception("boom");
    ContainerRemoveFailedException ex = new ContainerRemoveFailedException("c1", cause);

    assertEquals("Error while removing container 'c1'", ex.getMessage());
    assertSame(cause, ex.getCause());
  }
}
