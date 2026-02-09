package org.molgenis.armadillo.controller;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.dockerjava.api.DockerClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.molgenis.armadillo.TestSecurityConfig;
import org.molgenis.armadillo.exceptions.*;
import org.molgenis.armadillo.model.ArmadilloColumnMetaData;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.molgenis.armadillo.storage.FileInfo;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(StorageController.class)
@Import({TestSecurityConfig.class})
class StorageControllerTest extends ArmadilloControllerTestBase {

  @MockitoBean DockerClient dockerClient;
  @MockitoBean ArmadilloStorageService storage;

  @Captor protected ArgumentCaptor<InputStream> inputStreamCaptor;

  @Test
  @WithMockUser(roles = "SU")
  void testListObjects() throws Exception {
    when(storage.listObjects("lifecycle"))
        .thenReturn(List.of("core/nonrep.parquet", "outcome/nonrep.parquet"));

    mockMvc
        .perform(get("/storage/projects/lifecycle/objects").session(session))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[\"core/nonrep.parquet\", \"outcome/nonrep.parquet\"]"));

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant, "user", LIST_OBJECTS, mockSuAuditMap(Map.of(PROJECT, "lifecycle"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testUploadObject() throws Exception {
    var contents = "contents".getBytes();
    var file = mockMultipartFile(contents);

    mockMvc
        .perform(
            multipart("/storage/projects/lifecycle/objects")
                .file(file)
                .session(session)
                .param("object", "core/nonrep2.parquet"))
        .andExpect(status().isNoContent());

    verify(storage)
        .addObject(eq("lifecycle"), eq("core/nonrep2.parquet"), inputStreamCaptor.capture());
    assertArrayEquals(contents, inputStreamCaptor.getValue().readAllBytes());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            UPLOAD_OBJECT,
            mockSuAuditMap(Map.of(PROJECT, "lifecycle", OBJECT, "core/nonrep2.parquet"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testAddObjectFails() {
    InputStream mock = mock(InputStream.class);
    try {
      storage.addObject("test", "test", mock);
    } catch (Exception e) {
      assertEquals(e.getClass(), FileProcessingException.class);
    }
  }

  @Test
  @WithMockUser(roles = "SU")
  void testUploadCharacterSeparatedFile() throws Exception {
    var contents = "contents".getBytes();
    var file = mockMultipartFile(contents);
    mockMvc
        .perform(
            multipart("/storage/projects/lifecycle/csv")
                .file(file)
                .session(session)
                .param("object", "core/nonrep2.csv")
                .param("numberOfRowsToDetermineTypeBy", String.valueOf(10)))
        .andExpect(status().isNoContent());

    verify(storage).writeParquetFromCsv("lifecycle", "core/nonrep2.csv", file, 10);

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            UPLOAD_OBJECT,
            mockSuAuditMap(Map.of(PROJECT, "lifecycle", OBJECT, "core/nonrep2.csv"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testUploadCharacterSeparatedTsvFile() throws Exception {
    var contents = "contents".getBytes();
    var file = mockMultipartFile(contents);
    mockMvc
        .perform(
            multipart("/storage/projects/lifecycle/csv")
                .file(file)
                .session(session)
                .param("object", "core/nonrep2.tsv")
                .param("numberOfRowsToDetermineTypeBy", String.valueOf(10)))
        .andExpect(status().isNoContent());

    verify(storage).writeParquetFromCsv("lifecycle", "core/nonrep2.tsv", file, 10);

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            UPLOAD_OBJECT,
            mockSuAuditMap(Map.of(PROJECT, "lifecycle", OBJECT, "core/nonrep2.tsv"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testUploadCharacterSeparatedFileFails() throws Exception {
    var contents = "contents are broken,spaces in header not allowed".getBytes();
    var file = mockMultipartFile(contents);
    doThrow(new FileProcessingException("Cannot write parquet"))
        .when(storage)
        .writeParquetFromCsv("lifecycle", "core/nonrep2.csv", file, 10);
    mockMvc
        .perform(
            multipart("/storage/projects/lifecycle/csv")
                .file(file)
                .session(session)
                .param("object", "core/nonrep2.csv")
                .param("numberOfRowsToDetermineTypeBy", String.valueOf(10)))
        .andExpect(status().isBadRequest());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            UPLOAD_OBJECT + "_FAILURE",
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    OBJECT,
                    "core/nonrep2.csv",
                    "message",
                    "Could not process file: [data.parquet] because: [Cannot write parquet]",
                    "type",
                    "org.molgenis.armadillo.exceptions.FileProcessingException"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testUploadObjectProjectNotExists() throws Exception {
    var file = mockMultipartFile("contents".getBytes());
    doThrow(new UnknownProjectException("lifecycle"))
        .when(storage)
        .addObject(eq("lifecycle"), eq("core/nonrep2.parquet"), any(InputStream.class));

    mockMvc
        .perform(
            multipart("/storage/projects/lifecycle/objects")
                .file(file)
                .session(session)
                .param("object", "core/nonrep2.parquet"))
        .andExpect(status().isNotFound());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            UPLOAD_OBJECT + "_FAILURE",
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    OBJECT,
                    "core/nonrep2.parquet",
                    "message",
                    "Project 'lifecycle' does not exist",
                    "type",
                    "org.molgenis.armadillo.exceptions.UnknownProjectException"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testUploadObjectDuplicateObject() throws Exception {
    var file = mockMultipartFile("contents".getBytes());
    doThrow(new DuplicateObjectException("lifecycle", "core/nonrep2.parquet"))
        .when(storage)
        .addObject(eq("lifecycle"), eq("core/nonrep2.parquet"), any(InputStream.class));

    mockMvc
        .perform(
            multipart("/storage/projects/lifecycle/objects")
                .file(file)
                .session(session)
                .param("object", "core/nonrep2.parquet"))
        .andExpect(status().isConflict());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            UPLOAD_OBJECT + "_FAILURE",
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    OBJECT,
                    "core/nonrep2.parquet",
                    "message",
                    "Project 'lifecycle' already has an object 'core/nonrep2.parquet'",
                    "type",
                    "org.molgenis.armadillo.exceptions.DuplicateObjectException"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testCopyObject() throws Exception {
    mockMvc.perform(copyRequest()).andExpect(status().isNoContent());

    verify(storage).copyObject("lifecycle", "copies/test_copy.parquet", "test.parquet");

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            COPY_OBJECT,
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    "from",
                    "test.parquet",
                    "to",
                    "copies/test_copy.parquet"))));
  }

  private MockHttpServletRequestBuilder copyRequest() {
    return post("/storage/projects/lifecycle/objects/test.parquet/copy")
        .content("{\"name\": \"copies/test_copy.parquet\"}")
        .contentType(APPLICATION_JSON)
        .session(session);
  }

  @Test
  @WithMockUser(roles = "SU")
  void testCopyObjectNotExists() throws Exception {
    doThrow(new UnknownObjectException("lifecycle", "test.parquet"))
        .when(storage)
        .copyObject("lifecycle", "copies/test_copy.parquet", "test.parquet");

    mockMvc.perform(copyRequest()).andExpect(status().isNotFound());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            COPY_OBJECT + "_FAILURE",
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    "from",
                    "test.parquet",
                    "to",
                    "copies/test_copy.parquet",
                    "message",
                    "Project 'lifecycle' has no object 'test.parquet'",
                    "type",
                    "org.molgenis.armadillo.exceptions.UnknownObjectException"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testCopyObjectDuplicateObject() throws Exception {
    doThrow(new DuplicateObjectException("lifecycle", "copies/test_copy.parquet"))
        .when(storage)
        .copyObject("lifecycle", "copies/test_copy.parquet", "test.parquet");

    mockMvc.perform(copyRequest()).andExpect(status().isConflict());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            COPY_OBJECT + "_FAILURE",
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    "from",
                    "test.parquet",
                    "to",
                    "copies/test_copy.parquet",
                    "message",
                    "Project 'lifecycle' already has an object 'copies/test_copy.parquet'",
                    "type",
                    "org.molgenis.armadillo.exceptions.DuplicateObjectException"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testMoveObject() throws Exception {
    mockMvc.perform(moveRequest()).andExpect(status().isNoContent());

    verify(storage).moveObject("lifecycle", "test_renamed.parquet", "test.parquet");

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            MOVE_OBJECT,
            mockSuAuditMap(
                Map.of(
                    PROJECT, "lifecycle", "from", "test.parquet", "to", "test_renamed.parquet"))));
  }

  private MockHttpServletRequestBuilder moveRequest() {
    return post("/storage/projects/lifecycle/objects/test.parquet/move")
        .content("{\"name\": \"test_renamed.parquet\"}")
        .contentType(APPLICATION_JSON)
        .session(session);
  }

  @Test
  @WithMockUser(roles = "SU")
  void testMoveObjectNotExists() throws Exception {
    doThrow(new UnknownObjectException("lifecycle", "test.parquet"))
        .when(storage)
        .moveObject("lifecycle", "test_renamed.parquet", "test.parquet");

    mockMvc.perform(moveRequest()).andExpect(status().isNotFound());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            MOVE_OBJECT + "_FAILURE",
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    "from",
                    "test.parquet",
                    "to",
                    "test_renamed.parquet",
                    "message",
                    "Project 'lifecycle' has no object 'test.parquet'",
                    "type",
                    "org.molgenis.armadillo.exceptions.UnknownObjectException"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testMoveObjectDuplicateObject() throws Exception {
    doThrow(new DuplicateObjectException("lifecycle", "test_renamed.parquet"))
        .when(storage)
        .moveObject("lifecycle", "test_renamed.parquet", "test.parquet");

    mockMvc.perform(moveRequest()).andExpect(status().isConflict());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            MOVE_OBJECT + "_FAILURE",
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    "from",
                    "test.parquet",
                    "to",
                    "test_renamed.parquet",
                    "message",
                    "Project 'lifecycle' already has an object 'test_renamed.parquet'",
                    "type",
                    "org.molgenis.armadillo.exceptions.DuplicateObjectException"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testObjectExists() throws Exception {
    when(storage.hasObject("lifecycle", "test.parquet")).thenReturn(true);

    mockMvc
        .perform(head("/storage/projects/lifecycle/objects/test.parquet").session(session))
        .andExpect(status().isNoContent());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            GET_OBJECT,
            mockSuAuditMap(Map.of(PROJECT, "lifecycle", OBJECT, "test.parquet"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testObjectNotExists() throws Exception {
    when(storage.hasObject("lifecycle", "non-existing.parquet")).thenReturn(false);

    mockMvc
        .perform(head("/storage/projects/lifecycle/objects/non-existing.parquet").session(session))
        .andExpect(status().isNotFound());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            GET_OBJECT,
            mockSuAuditMap(Map.of(PROJECT, "lifecycle", OBJECT, "non-existing.parquet"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testDeleteObject() throws Exception {
    mockMvc
        .perform(
            delete(new URI("/storage/projects/lifecycle/objects/test.parquet")).session(session))
        .andExpect(status().isNoContent());

    verify(storage).deleteObject("lifecycle", "test.parquet");

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            DELETE_OBJECT,
            mockSuAuditMap(Map.of(PROJECT, "lifecycle", OBJECT, "test.parquet"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testDeleteObjectNotExists() throws Exception {
    doThrow(new UnknownObjectException("lifecycle", "test.parquet"))
        .when(storage)
        .deleteObject("lifecycle", "test.parquet");

    mockMvc
        .perform(delete("/storage/projects/lifecycle/objects/test.parquet").session(session))
        .andExpect(status().isNotFound());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            DELETE_OBJECT + "_FAILURE",
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    OBJECT,
                    "test.parquet",
                    "message",
                    "Project 'lifecycle' has no object 'test.parquet'",
                    "type",
                    "org.molgenis.armadillo.exceptions.UnknownObjectException"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testDownloadObject() throws Exception {
    var content = "content".getBytes();
    var inputStream = new ByteArrayInputStream(content);
    when(storage.loadObject("lifecycle", "test.parquet")).thenReturn(inputStream);
    when(storage.getFileSizeIfObjectExists("shared-lifecycle", "test.parquet")).thenReturn(12345L);

    mockMvc
        .perform(get("/storage/projects/lifecycle/objects/test.parquet").session(session))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
        .andExpect(content().bytes(content));

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            DOWNLOAD_OBJECT,
            mockSuAuditMap(Map.of(PROJECT, "lifecycle", OBJECT, "test.parquet"))));
  }

  @Test
  @WithMockUser(roles = "LIFECYCLE_RESEARCHER")
  void testDownloadObjectAsResearcher() throws Exception {
    var content = "content".getBytes();
    var inputStream = new ByteArrayInputStream(content);
    when(storage.loadObject("lifecycle", "test.parquet")).thenReturn(inputStream);
    when(storage.getFileSizeIfObjectExists("shared-lifecycle", "test.parquet")).thenReturn(12345L);

    mockMvc
        .perform(get("/storage/projects/lifecycle/objects/test.parquet").session(session))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "SU")
  void testPreviewObject() throws Exception {
    when(storage.getPreview("lifecycle", "test.parquet")).thenReturn(List.of(Map.of("foo", "bar")));

    mockMvc
        .perform(get("/storage/projects/lifecycle/objects/test.parquet/preview").session(session))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[{\"foo\": \"bar\"}]"));

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            PREVIEW_OBJECT,
            mockSuAuditMap(Map.of(PROJECT, "lifecycle", OBJECT, "test.parquet"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testCreateLinkedObject() throws Exception {
    doNothing()
        .when(storage)
        .createLinkedObject("lifecycle", "test", "my-link", "lifecycle", "a,b,c");
    mockMvc
        .perform(
            post("/storage/projects/lifecycle/objects/link")
                .content(
                    "{\"sourceProject\": \"lifecycle\", \"sourceObjectName\": \"test\", \"linkedObject\": \"my-link\", \"variables\": \"a,b,c\"}")
                .contentType(APPLICATION_JSON)
                .session(session))
        .andExpect(status().isNoContent());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            CREATE_LINKED_OBJECT,
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    "source",
                    "lifecycle/my-link",
                    "columns",
                    List.of("a", "b", "c"),
                    OBJECT,
                    "my-link.alf"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testGetObjectInfo() throws Exception {
    when(storage.getInfo("lifecycle", "test.parquet"))
        .thenReturn(new FileInfo("test.parquet", "5 MB", "20000", "30", null, new String[] {}));

    mockMvc
        .perform(get("/storage/projects/lifecycle/objects/test.parquet/info").session(session))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    "{\"name\": \"test.parquet\", \"size\": \"5 MB\", \"rows\": \"20000\", \"columns\": \"30\"}"));

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            GET_OBJECT_INFO,
            mockSuAuditMap(Map.of(PROJECT, "lifecycle", OBJECT, "test.parquet"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testDownloadObjectNotExists() throws Exception {
    doThrow(new UnknownObjectException("lifecycle", "test.parquet"))
        .when(storage)
        .loadObject("lifecycle", "test.parquet");

    mockMvc
        .perform(get("/storage/projects/lifecycle/objects/test.parquet").session(session))
        .andExpect(status().isNotFound());

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            DOWNLOAD_OBJECT + "_FAILURE",
            mockSuAuditMap(
                Map.of(
                    PROJECT,
                    "lifecycle",
                    OBJECT,
                    "test.parquet",
                    "message",
                    "Project 'lifecycle' has no object 'test.parquet'",
                    "type",
                    "org.molgenis.armadillo.exceptions.UnknownObjectException"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testGetVariables() throws Exception {
    when(storage.getVariables("my-project", "my-table.parquet"))
        .thenReturn(List.of("col1", "col2", "col3"));

    mockMvc
        .perform(
            get("/storage/projects/my-project/objects/my-table.parquet/variables").session(session))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(content().json("[\"col1\", \"col2\", \"col3\"]"));

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            GET_VARIABLES,
            mockSuAuditMap(Map.of(PROJECT, "my-project", OBJECT, "my-table.parquet"))));
  }

  @Test
  @WithMockUser(roles = "SU")
  void testGetMetadataOfTable() throws Exception {
    ArmadilloColumnMetaData armadilloColumn1 = ArmadilloColumnMetaData.create("INT32");
    ArmadilloColumnMetaData armadilloColumn2 = ArmadilloColumnMetaData.create("BINARY");

    Map<String, ArmadilloColumnMetaData> metadata =
        Map.of(
            "column1", armadilloColumn1,
            "column2", armadilloColumn2);

    when(storage.getMetadata("lifecycle", "test.parquet")).thenReturn(metadata);

    mockMvc
        .perform(get("/storage/projects/lifecycle/objects/test.parquet/metadata").session(session))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_JSON))
        .andExpect(
            content()
                .json(
                    """
                                    {
                                      "column1": {"type": "INT32", "missing":  "0/0"},
                                      "column2": {"type": "BINARY", "missing":  "0/0", "levels":  []}
                                    }
                                    """,
                    JsonCompareMode.STRICT));

    auditEventValidator.validateAuditEvent(
        new AuditEvent(
            instant,
            "user",
            PREVIEW_METADATA,
            mockSuAuditMap(Map.of(PROJECT, "lifecycle", OBJECT, "test.parquet"))));
  }

  private Map<String, Object> mockSuAuditMap() {
    var values = new HashMap<String, Object>();
    values.put("sessionId", sessionId);
    values.put("roles", List.of("ROLE_SU"));
    return values;
  }

  private Map<String, Object> mockSuAuditMap(Map<String, Object> additionalValues) {
    var values = mockSuAuditMap();
    values.putAll(additionalValues);
    return values;
  }

  private MockMultipartFile mockMultipartFile(byte[] contents) throws IOException {
    return new MockMultipartFile(
        "file",
        "data.parquet",
        MediaType.MULTIPART_FORM_DATA_VALUE,
        new ByteArrayInputStream(contents));
  }

  @Test
  void testDownloadRawfileWithResourceToken() throws Exception {
    var content = "content".getBytes();
    var inputStream = new ByteArrayInputStream(content);

    when(storage.loadObject("lifecycle", "test.parquet")).thenReturn(inputStream);
    when(storage.getFileSizeIfObjectExists("shared-lifecycle", "test.parquet")).thenReturn(12345L);

    mockMvc
        .perform(
            get("/storage/projects/lifecycle/rawfiles/test.parquet")
                .with(
                    jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_RESOURCE_VIEW"))
                        .jwt(
                            builder ->
                                builder
                                    .subject("user@example.com")
                                    .claim("email", "user@example.com")
                                    .claim("iss", "http://armadillo-internal")
                                    .claim("resource_project", "lifecycle")
                                    .claim("resource_object", "test"))))
        .andExpect(status().isOk())
        .andExpect(content().contentType(APPLICATION_OCTET_STREAM))
        .andExpect(content().bytes(content));
  }

  @Test
  void testDownloadRawfileWrongProjectForbidden() throws Exception {
    mockMvc
        .perform(
            get("/storage/projects/lifecycle/rawfiles/test.parquet")
                .with(
                    jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_RESOURCE_VIEW"))
                        .jwt(
                            builder ->
                                builder
                                    .subject("user@example.com")
                                    .claim("email", "user@example.com")
                                    .claim("iss", "http://armadillo-internal")
                                    .claim("resource_project", "other-project")
                                    .claim("resource_object", "test"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void testDownloadRawfileWrongObjectForbidden() throws Exception {
    mockMvc
        .perform(
            get("/storage/projects/lifecycle/rawfiles/test.parquet")
                .with(
                    jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_RESOURCE_VIEW"))
                        .jwt(
                            builder ->
                                builder
                                    .subject("user@example.com")
                                    .claim("email", "user@example.com")
                                    .claim("iss", "http://armadillo-internal")
                                    .claim("resource_project", "lifecycle")
                                    .claim("resource_object", "other"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void testDownloadRawfileWrongIssuerForbidden() throws Exception {
    mockMvc
        .perform(
            get("/storage/projects/lifecycle/rawfiles/test.parquet")
                .with(
                    jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_RESOURCE_VIEW"))
                        .jwt(
                            builder ->
                                builder
                                    .subject("user@example.com")
                                    .claim("email", "user@example.com")
                                    .claim("iss", "http://some-other-app")
                                    .claim("resource_project", "lifecycle")
                                    .claim("resource_object", "test"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void testDownloadRawfileNoTokenUnauthorized() throws Exception {
    mockMvc
        .perform(get("/storage/projects/lifecycle/rawfiles/test.parquet"))
        .andExpect(status().isUnauthorized());
  }
}
