package org.molgenis.armadillo.minio;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.messages.Item;
import java.security.Principal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.model.Workspace;

@ExtendWith(MockitoExtension.class)
class ArmadilloStorageServiceTest {

  @Mock MinioStorageService storageService;
  @Mock Principal principal;
  @Mock Item item;
  ArmadilloStorageService armadilloStorage;

  @BeforeEach
  void setup() {
    armadilloStorage = new ArmadilloStorageService(storageService);
  }

  @Test
  void testListWorkspaces() {
    when(principal.getName()).thenReturn("henk");
    Instant lastModified = Instant.now().truncatedTo(MILLIS);
    Workspace workspace =
        Workspace.builder()
            .setName("blah")
            .setLastModified(lastModified)
            .setETag("\"abcde\"")
            .setSize(56)
            .build();

    when(storageService.listObjects("user-henk")).thenReturn(List.of(item));
    when(item.objectName()).thenReturn("blah.RData");
    when(item.lastModified()).thenReturn(Date.from(lastModified));
    when(item.etag()).thenReturn(workspace.eTag());
    when(item.objectSize()).thenReturn(workspace.size());

    assertEquals(List.of(workspace), armadilloStorage.listWorkspaces(principal));
  }

  @Test
  void testDeleteWorkspace() {
    when(principal.getName()).thenReturn("henk");
    armadilloStorage.removeWorkspace(principal, "test");

    verify(storageService).delete("user-henk", "test.RData");
  }
}
