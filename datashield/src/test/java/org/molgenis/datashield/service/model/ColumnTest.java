package org.molgenis.datashield.service.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.molgenis.r.model.Column;
import org.molgenis.r.model.ColumnType;

class ColumnTest {
  @Test
  public void testCheckName() {
    assertThrows(
        IllegalStateException.class,
        () -> Column.builder().setName("Nöt Valid").setType(ColumnType.DATE).build());
  }
}
