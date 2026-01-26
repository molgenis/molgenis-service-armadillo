package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ContainerScopeAnnotationTest {

  @Test
  void containerScope_isAnnotation() {
    assertTrue(org.molgenis.armadillo.container.annotation.ContainerScope.class.isAnnotation());
  }
}
