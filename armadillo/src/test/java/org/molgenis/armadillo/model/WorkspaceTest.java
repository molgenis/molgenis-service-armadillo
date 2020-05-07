package org.molgenis.armadillo.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class WorkspaceTest {
  private Workspace workspace =
      Workspace.builder()
          .setName("admin/name.RData")
          .setLastModified(Instant.now())
          .setETag("etag")
          .setSize(1234)
          .build();

  @Test
  public void testTrim() {
    workspace = workspace.trim("admin/", ".RData");
    assertEquals("name", workspace.name());
  }

  @Test
  public void testTrimWrongPrefix() {
    assertThrows(IllegalArgumentException.class, () -> workspace.trim("user", ".RData"));
  }

  @Test
  public void testTrimWrongPostfix() {
    assertThrows(IllegalArgumentException.class, () -> workspace.trim("admin", ".notRData"));
  }
}
