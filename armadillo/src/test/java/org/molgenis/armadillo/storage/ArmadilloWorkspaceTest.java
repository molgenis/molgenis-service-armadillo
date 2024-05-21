package org.molgenis.armadillo.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.exceptions.StorageException;

public class ArmadilloWorkspaceTest {
  @Test
  public void testGetFileName() {
    InputStream stubInputStream =
        IOUtils.toInputStream("some test data for my input stream", "UTF-8");
    ArmadilloWorkspace workspace = new ArmadilloWorkspace(stubInputStream);
    long size = workspace.getSize();
    assertEquals(34, size);
  }

  @Test
  void testGetByteOfInputStreamThrowsError() throws IOException {
    InputStream isMock = mock(InputStream.class);
    when(isMock.read(any(byte[].class))).thenThrow(IOException.class);
    assertThrows(StorageException.class, () -> new ArmadilloWorkspace(isMock));
  }
}
