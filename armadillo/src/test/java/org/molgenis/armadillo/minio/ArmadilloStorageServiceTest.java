package org.molgenis.armadillo.minio;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import io.minio.messages.Bucket;
import io.minio.messages.Item;
import java.io.InputStream;
import java.security.Principal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ExtendWith(MockitoExtension.class)
class ArmadilloStorageServiceTest {
  @MockBean MinioStorageService storageService;
  @Mock Principal principal;
  @Mock Item item;
  @Mock Bucket gecko;
  @Mock Bucket diabetes;
  @Mock InputStream is;
  @Autowired ArmadilloStorageService armadilloStorage;

  @EnableGlobalMethodSecurity(prePostEnabled = true)
  @Configuration
  static class Config {
    @Bean
    ArmadilloStorageService armadilloStorageService(MinioStorageService storageService) {
      return new ArmadilloStorageService(storageService);
    }
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testListProjects() {
    when(gecko.name()).thenReturn("shared-gecko");
    when(diabetes.name()).thenReturn("shared-diabetes");
    when(storageService.listBuckets()).thenReturn(List.of(gecko, diabetes));
    assertEquals(List.of("gecko"), armadilloStorage.listProjects());
  }

  @Test
  @WithMockUser
  void testListTablesChecksPermission() {
    assertThrows(AccessDeniedException.class, () -> armadilloStorage.listTables("gecko"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testListTablesAllowsSuperUser() {
    assertDoesNotThrow(() -> armadilloStorage.listTables("gecko"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testListTablesAllowsResearcher() {
    assertDoesNotThrow(() -> armadilloStorage.listTables("gecko"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testListTablesListsObjectsInSharedBucket() {
    when(storageService.listObjects("shared-gecko")).thenReturn(List.of(item));
    when(item.objectName()).thenReturn("1_0_release_1_1/gecko.parquet");
    assertEquals(List.of("gecko/1_0_release_1_1/gecko"), armadilloStorage.listTables("gecko"));
  }

  @Test
  @WithMockUser
  void testTableExistsChecksPermission() {
    assertThrows(
        AccessDeniedException.class,
        () -> armadilloStorage.tableExists("gecko", "1_0_release_1_1/gecko"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testTableExistsAllowsSuperUser() {
    assertDoesNotThrow(() -> armadilloStorage.tableExists("gecko", "1_0_release_1_1/gecko"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testTableExistsAllowsResearcher() {
    assertDoesNotThrow(() -> armadilloStorage.tableExists("gecko", "1_0_release_1_1/gecko"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testTableExistsChecksExistence() {
    when(storageService.objectExists("shared-gecko", "1_0_release_1_1/gecko.parquet"))
        .thenReturn(true);
    assertTrue(armadilloStorage.tableExists("gecko", "1_0_release_1_1/gecko"));
  }

  @Test
  @WithMockUser
  void testLoadTableChecksPermission() {
    assertThrows(
        AccessDeniedException.class,
        () -> armadilloStorage.loadTable("gecko", "1_0_release_1_1/gecko"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testLoadTableAllowsSuperUser() {
    assertDoesNotThrow(() -> armadilloStorage.loadTable("gecko", "1_0_release_1_1/gecko"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testLoadTableAllowsResearcher() {
    assertDoesNotThrow(() -> armadilloStorage.loadTable("gecko", "1_0_release_1_1/gecko"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testLoadTableLoadsTable() {
    when(storageService.load("shared-gecko", "1_0_release_1_1/gecko.parquet")).thenReturn(is);
    assertSame(is, armadilloStorage.loadTable("gecko", "1_0_release_1_1/gecko"));
  }

  @Test
  @WithMockUser
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

  @Test
  void testLoadWorkspace() {
    when(principal.getName()).thenReturn("henk");
    when(storageService.load("user-henk", "test.RData")).thenReturn(is);

    assertSame(is, armadilloStorage.loadWorkspace(principal, "test"));
  }

  @Test
  void testSaveWorkspace() {
    when(principal.getName()).thenReturn("henk");

    armadilloStorage.saveWorkspace(is, principal, "test");

    verify(storageService).save(is, "user-henk", "test.RData", APPLICATION_OCTET_STREAM);
  }

  @Test
  void testSaveWorkspaceChecksBucketName() {
    when(principal.getName()).thenReturn("Henk");

    assertThrows(
        IllegalArgumentException.class,
        () -> armadilloStorage.saveWorkspace(is, principal, "test"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testResourceExists() {
    when(storageService.objectExists("shared-gecko", "hpc-resource.rds")).thenReturn(true);
    boolean exists = armadilloStorage.resourceExists("gecko", "hpc-resource");
    assertTrue(exists);
  }

  @Test
  @WithMockUser(roles = "SU")
  void testLoadResource() {
    when(storageService.load("shared-gecko", "hpc-resource.rds")).thenReturn(is);

    InputStream resource = armadilloStorage.loadResource("gecko", "hpc-resource");
    assertNotNull(resource);
  }

  @Test
  @WithMockUser(roles = "SU")
  void testListResources() {
    when(storageService.listObjects("shared-gecko")).thenReturn(List.of(item));
    when(item.objectName()).thenReturn("hpc-resource.rds");

    assertEquals(List.of("gecko/hpc-resource"), armadilloStorage.listResources("gecko"));
  }
}
