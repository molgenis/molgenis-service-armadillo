package org.molgenis.armadillo.storage;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.SYSTEM;
import static org.molgenis.armadillo.storage.ArmadilloStorageService.validateProjectName;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.DuplicateObjectException;
import org.molgenis.armadillo.exceptions.InvalidProjectNameException;
import org.molgenis.armadillo.exceptions.UnknownObjectException;
import org.molgenis.armadillo.exceptions.UnknownProjectException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ExtendWith(MockitoExtension.class)
class ArmadilloStorageServiceTest {

  final String SHARED_GECKO = "shared-gecko";
  final String SHARED_DIABETES = "shared-diabetes";
  final String METADATA_FILE = "metadata.json";

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
  @WithMockUser(roles = "SU")
  void testUpsertProject() {
    armadilloStorage.upsertProject("test");
    verify(storageService).createBucketIfNotExists("shared-test");
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testUpsertProjectDenied() {
    assertThrows(AccessDeniedException.class, () -> armadilloStorage.upsertProject("test"));
    verify(storageService, never()).createBucketIfNotExists(any(String.class));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testHasProject() {
    when(storageService.listBuckets()).thenReturn(List.of("shared-test", "shared-lifecycle"));
    assertTrue(armadilloStorage.hasProject("test"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testHasProjectDenied() {
    assertThrows(AccessDeniedException.class, () -> armadilloStorage.hasProject("test"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testDeleteProject() {
    when(storageService.listBuckets()).thenReturn(List.of("shared-test"));
    armadilloStorage.deleteProject("test");
    verify(storageService).deleteBucket("shared-test");
  }

  @Test
  @WithMockUser(roles = "SU")
  void testDeleteProjectNotExists() {
    when(storageService.listBuckets()).thenReturn(List.of());
    assertThrows(UnknownProjectException.class, () -> armadilloStorage.deleteProject("test"));
    verify(storageService, never()).deleteBucket(any(String.class));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testDeleteProjectDenied() {
    assertThrows(AccessDeniedException.class, () -> armadilloStorage.deleteProject("test"));
    verify(storageService, never()).deleteBucket(any(String.class));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testAddObject() {
    when(storageService.listBuckets()).thenReturn(List.of("shared-test"));
    var inputStream = mock(InputStream.class);

    armadilloStorage.addObject("test", "core/test.parquet", inputStream);

    verify(storageService)
        .save(inputStream, "shared-test", "core/test.parquet", APPLICATION_OCTET_STREAM);
  }

  @Test
  @WithMockUser(roles = "SU")
  void testAddObjectProjectNotExists() {
    when(storageService.listBuckets()).thenReturn(List.of());
    assertThrows(
        UnknownProjectException.class,
        () -> armadilloStorage.addObject("test", "core/test.parquet", mock(InputStream.class)));
    verifyNoObjectSaved();
  }

  @Test
  @WithMockUser(roles = "SU")
  void testAddObjectDuplicate() {
    mockExistingObject("shared-test", "core/test.parquet");
    when(storageService.listBuckets()).thenReturn(List.of("shared-test"));

    assertThrows(
        DuplicateObjectException.class,
        () -> armadilloStorage.addObject("test", "core/test.parquet", mock(InputStream.class)));

    verifyNoObjectSaved();
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testAddObjectDenied() {
    when(storageService.listBuckets()).thenReturn(List.of("shared-test"));
    assertThrows(
        AccessDeniedException.class,
        () -> armadilloStorage.addObject("test", "core/test.parquet", mock(InputStream.class)));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testHasObject() {
    mockExistingObject("shared-test", "test.parquet");
    assertTrue(armadilloStorage.hasObject("test", "test.parquet"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testHasObjectDenied() {
    assertThrows(
        AccessDeniedException.class, () -> armadilloStorage.hasObject("test", "core/test.parquet"));
  }

  @Test
  @WithMockUser(roles = "TEST_RESEARCHER")
  void testHasObjectUser() {
    mockExistingObject("shared-test", "test.parquet");
    assertTrue(armadilloStorage.hasObject("test", "test.parquet"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testHasObjectProjectNotExists() {
    when(storageService.listBuckets()).thenReturn(List.of());
    assertThrows(
        UnknownProjectException.class,
        () -> armadilloStorage.hasObject("test", "core/test.parquet"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testMoveObject() {
    var inputStream = mock(InputStream.class);
    mockExistingObject("shared-test", "test.parquet");
    when(storageService.load("shared-test", "test.parquet")).thenReturn(inputStream);

    armadilloStorage.moveObject("test", "renamed.parquet", "test.parquet");

    verify(storageService)
        .save(inputStream, "shared-test", "renamed.parquet", APPLICATION_OCTET_STREAM);
    verify(storageService).delete("shared-test", "test.parquet");
  }

  @Test
  @WithMockUser(roles = "SU")
  void testMoveObjectProjectNotExists() {
    when(storageService.listBuckets()).thenReturn(List.of());

    assertThrows(
        UnknownProjectException.class,
        () -> armadilloStorage.moveObject("test", "renamed.parquet", "test.parquet"));

    verifyNoObjectLoaded();
    verifyNoObjectSaved();
    verifyNoObjectDeleted();
  }

  @Test
  @WithMockUser(roles = "SU")
  void testMoveObjectNotExists() {
    when(storageService.listBuckets()).thenReturn(List.of("shared-test"));

    assertThrows(
        UnknownObjectException.class,
        () -> armadilloStorage.moveObject("test", "renamed.parquet", "test.parquet"));

    verifyNoObjectLoaded();
    verifyNoObjectSaved();
    verifyNoObjectDeleted();
  }

  @Test
  @WithMockUser(roles = "SU")
  void testMoveObjectDuplicate() {
    mockExistingTestObjects("shared-test", List.of("test.parquet", "renamed.parquet"));

    assertThrows(
        DuplicateObjectException.class,
        () -> armadilloStorage.moveObject("test", "renamed.parquet", "test.parquet"));

    verifyNoObjectLoaded();
    verifyNoObjectSaved();
    verifyNoObjectDeleted();
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testMoveObjectDenied() {
    assertThrows(
        AccessDeniedException.class,
        () -> armadilloStorage.moveObject("test", "renamed.parquet", "test.parquet"));

    verifyNoObjectLoaded();
    verifyNoObjectSaved();
    verifyNoObjectDeleted();
  }

  @Test
  @WithMockUser(roles = "SU")
  void testCopyObject() {
    var inputStream = mock(InputStream.class);
    mockExistingObject("shared-test", "test.parquet");
    when(storageService.load("shared-test", "test.parquet")).thenReturn(inputStream);

    armadilloStorage.copyObject("test", "copy.parquet", "test.parquet");

    verify(storageService)
        .save(inputStream, "shared-test", "copy.parquet", APPLICATION_OCTET_STREAM);
    verifyNoObjectDeleted();
  }

  @Test
  @WithMockUser(roles = "SU")
  void testCopyObjectProjectNotExists() {
    when(storageService.listBuckets()).thenReturn(List.of());

    assertThrows(
        UnknownProjectException.class,
        () -> armadilloStorage.copyObject("test", "copy.parquet", "test.parquet"));

    verifyNoObjectLoaded();
    verifyNoObjectSaved();
  }

  @Test
  @WithMockUser(roles = "SU")
  void testCopyObjectNotExists() {
    when(storageService.listBuckets()).thenReturn(List.of("shared-test"));

    assertThrows(
        UnknownObjectException.class,
        () -> armadilloStorage.copyObject("test", "copy.parquet", "test.parquet"));

    verifyNoObjectLoaded();
    verifyNoObjectSaved();
  }

  @Test
  @WithMockUser(roles = "SU")
  void testCopyObjectDuplicate() {
    mockExistingTestObjects("shared-test", List.of("test.parquet", "copy.parquet"));

    assertThrows(
        DuplicateObjectException.class,
        () -> armadilloStorage.copyObject("test", "copy.parquet", "test.parquet"));

    verifyNoObjectLoaded();
    verifyNoObjectSaved();
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testCopyObjectDenied() {
    assertThrows(
        AccessDeniedException.class,
        () -> armadilloStorage.copyObject("test", "copy.parquet", "test.parquet"));

    verifyNoObjectLoaded();
    verifyNoObjectSaved();
  }

  @Test
  @WithMockUser(roles = "SU")
  void testDeleteObject() {
    mockExistingObject("shared-test", "test.parquet");
    armadilloStorage.deleteObject("test", "test.parquet");
    verify(storageService).delete("shared-test", "test.parquet");
  }

  @Test
  @WithMockUser(roles = "SU")
  void testDeleteObjectNotExists() {
    when(storageService.listBuckets()).thenReturn(List.of("shared-test"));
    assertThrows(
        UnknownObjectException.class, () -> armadilloStorage.deleteObject("test", "test.parquet"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testDeleteObjectDenied() {
    assertThrows(
        AccessDeniedException.class, () -> armadilloStorage.deleteObject("test", "test.parquet"));
    verifyNoObjectDeleted();
  }

  @Test
  @WithMockUser(roles = "SU")
  void testLoadObject() {
    mockExistingObject("shared-test", "test.parquet");
    var inputStream = mock(InputStream.class);
    when(storageService.load("shared-test", "test.parquet")).thenReturn(inputStream);

    assertEquals(inputStream, armadilloStorage.loadObject("test", "test.parquet"));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testLoadObjectNotExists() {
    when(storageService.listBuckets()).thenReturn(List.of("shared-test"));
    assertThrows(
        UnknownObjectException.class, () -> armadilloStorage.loadObject("test", "test.parquet"));
    verifyNoObjectLoaded();
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testLoadObjectDenied() {
    assertThrows(
        AccessDeniedException.class, () -> armadilloStorage.loadObject("test", "test.parquet"));
    verifyNoObjectLoaded();
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testListProjects() {
    when(storageService.listBuckets()).thenReturn(List.of(SHARED_GECKO, SHARED_DIABETES));
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
    when(storageService.listBuckets()).thenReturn(singletonList(SHARED_GECKO));
    assertDoesNotThrow(() -> armadilloStorage.listTables("gecko"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testListTablesAllowsResearcher() {
    when(storageService.listBuckets()).thenReturn(singletonList(SHARED_GECKO));
    assertDoesNotThrow(() -> armadilloStorage.listTables("gecko"));
  }

  @Test
  @WithMockUser(roles = "GECKO_RESEARCHER")
  void testListTablesListsObjectsInSharedBucket() {
    when(storageService.listBuckets()).thenReturn(singletonList(SHARED_GECKO));
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
    when(storageService.objectExists(SHARED_GECKO, "1_0_release_1_1/gecko.parquet"))
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
    when(storageService.load(SHARED_GECKO, "1_0_release_1_1/gecko.parquet")).thenReturn(is);
    assertSame(is, armadilloStorage.loadTable("gecko", "1_0_release_1_1/gecko"));
  }

  //  @Test
  //  @WithMockUser
  //  void testListWorkspaces() {
  //    when(principal.getName()).thenReturn("henk");
  //    Instant lastModified = Instant.now().truncatedTo(MILLIS);
  //    Workspace workspace =
  //        Workspace.builder().setName("blah").setLastModified(lastModified).setSize(56).build();
  //
  //    when(storageService.listObjects("user-henk")).thenReturn(List.of(item));
  //    when(item.name()).thenReturn("blah.RData");
  //    when(item.lastModified()).thenReturn(Date.from(lastModified));
  //    when(item.size()).thenReturn(workspace.size());
  //
  //    assertEquals(List.of(workspace), armadilloStorage.listWorkspaces(principal));
  //  }

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
    when(storageService.objectExists(SHARED_GECKO, "hpc-resource.rds")).thenReturn(true);
    boolean exists = armadilloStorage.resourceExists("gecko", "hpc-resource");
    assertTrue(exists);
  }

  @Test
  @WithMockUser(roles = "SU")
  void testLoadResource() {
    when(storageService.load(SHARED_GECKO, "hpc-resource.rds")).thenReturn(is);

    InputStream resource = armadilloStorage.loadResource("gecko", "hpc-resource");
    assertNotNull(resource);
  }

  @Test
  @WithMockUser(roles = "SU")
  void testListResources() {
    when(storageService.listBuckets()).thenReturn(singletonList(SHARED_GECKO));
    when(storageService.listObjects(SHARED_GECKO)).thenReturn(List.of(item));
    when(item.name()).thenReturn("hpc-resource.rds");

    assertEquals(List.of("gecko/hpc-resource"), armadilloStorage.listResources("gecko"));
  }

  @Test
  void testLoadSystemFile() throws IOException {
    String testValue = "test";
    when(storageService.load(SYSTEM, METADATA_FILE))
        .thenReturn(new ByteArrayInputStream(testValue.getBytes()));
    when(storageService.objectExists(SYSTEM, METADATA_FILE)).thenReturn(true);
    InputStream result = armadilloStorage.loadSystemFile(METADATA_FILE);
    assertEquals(testValue, new String(result.readAllBytes()));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testSaveSystemFile() {
    String testValue = "test";
    ArgumentCaptor<ByteArrayInputStream> argument =
        ArgumentCaptor.forClass(ByteArrayInputStream.class);
    armadilloStorage.saveSystemFile(
        new ByteArrayInputStream(testValue.getBytes()), METADATA_FILE, APPLICATION_JSON);
    verify(storageService)
        .save(argument.capture(), eq(SYSTEM), eq(METADATA_FILE), eq(APPLICATION_JSON));
    assertEquals(testValue, new String(argument.getValue().readAllBytes()));
  }

  @Test
  void testValidateProjectName() {
    assertDoesNotThrow(() -> validateProjectName("lifecycle"));
  }

  @Test
  void testValidateProjectNameUppercase() {
    assertThrows(InvalidProjectNameException.class, () -> validateProjectName("Lifecycle"));
  }

  @Test
  void testValidateProjectNameNull() {
    assertThrows(NullPointerException.class, () -> validateProjectName(null));
  }

  @Test
  void testValidateProjectNameIllegalChar() {
    assertThrows(InvalidProjectNameException.class, () -> validateProjectName("illegal~name"));
  }

  @SuppressWarnings("SameParameterValue")
  private void mockExistingObject(String bucketName, String objectName) {
    mockExistingTestObjects(bucketName, List.of(objectName));
  }

  private void mockExistingTestObjects(String bucketName, List<String> objectNames) {
    objectNames.forEach(
        name -> when(storageService.objectExists(bucketName, name)).thenReturn(true));
    when(storageService.listBuckets()).thenReturn(List.of(bucketName));
  }

  private void verifyNoObjectDeleted() {
    verify(storageService, never()).delete(any(String.class), any(String.class));
  }

  private void verifyNoObjectSaved() {
    verify(storageService, never())
        .save(any(InputStream.class), any(String.class), any(String.class), any(MediaType.class));
  }

  private void verifyNoObjectLoaded() {
    verify(storageService, never()).load(any(String.class), any(String.class));
  }
}
