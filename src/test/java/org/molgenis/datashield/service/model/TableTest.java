package org.molgenis.datashield.service.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TableTest {
  @Test
  public void testChecksName() {
    assertThrows(IllegalStateException.class, () -> Table.builder().setName("No spaces").build());
  }
}
