package org.molgenis.armadillo.storage;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.settings.ArmadilloSettingsService.SETTINGS_FILE;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.SYSTEM;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
  final String SHARED_GECKO = "shared-gecko";
  final String SHARED_DIABETES = "shared-diabetes";

  @MockBean StorageService storageService;
  @Mock Principal principal;
  @Mock ObjectMetadata item;
  @Mock InputStream is;
  @Autowired ArmadilloStorageService armadilloStorage;

  @EnableGlobalMethodSecurity(prePostEnabled = true)
  @Configuration
  static class Config {
    @Bean
    ArmadilloStorageService armadilloStorageService(StorageService storageService) {
      return new ArmadilloStorageService(storageService);
    }
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testListProjects() {
    when(storageService.listProjects()).thenReturn(List.of(SHARED_GECKO, SHARED_DIABETES));
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
    when(storageService.listObjects(SHARED_GECKO)).thenReturn(List.of(item));
    when(item.name()).thenReturn("1_0_release_1_1/gecko.parquet");
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
        Workspace.builder().setName("blah").setLastModified(lastModified).setSize(56).build();

    when(storageService.listObjects("user-henk")).thenReturn(List.of(item));
    when(item.name()).thenReturn("blah.RData");
    when(item.lastModified()).thenReturn(Date.from(lastModified));
    when(item.size()).thenReturn(workspace.size());

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
    when(storageService.listObjects(SHARED_GECKO)).thenReturn(List.of(item));
    when(item.name()).thenReturn("hpc-resource.rds");

    assertEquals(List.of("gecko/hpc-resource"), armadilloStorage.listResources("gecko"));
  }

  @Test
  void testLoadSystemFile() throws IOException {
    String testValue = "test";
    when(storageService.load(SYSTEM, SETTINGS_FILE))
        .thenReturn(new ByteArrayInputStream(testValue.getBytes()));
    when(storageService.objectExists(SYSTEM, SETTINGS_FILE)).thenReturn(true);
    InputStream result = armadilloStorage.loadSystemFile(SETTINGS_FILE);
    assertEquals(testValue, new String(result.readAllBytes()));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testSaveSystemFile() {
    String testValue = "test";
    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    armadilloStorage.saveSystemFile(
        new ByteArrayInputStream(testValue.getBytes()), SETTINGS_FILE, APPLICATION_JSON);
    verify(storageService)
        .save(argument.capture(), eq(SYSTEM), eq(SETTINGS_FILE), eq(APPLICATION_JSON));
    assertEquals(testValue, new String(argument.getValue().readAllBytes()));
  }
}
