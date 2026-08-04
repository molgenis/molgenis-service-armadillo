package org.molgenis.armadillo.controller;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.function.Predicate.not;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.molgenis.armadillo.audit.AuditEventPublisher.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.molgenis.armadillo.audit.AuditEventPublisher;
import org.molgenis.armadillo.config.ConfigFile;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.molgenis.armadillo.service.RebootScriptRunner;
import org.molgenis.armadillo.storage.FileDownloader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ManagementControllerITTest {

  public static final Gson GSON = new Gson();
  @MockitoSpyBean AuditEventPublisher auditor;

  @Value("${armadillo.armadillo-home:/usr/share/armadillo/application}")
  String armadilloHome;

  @TempDir static java.nio.file.Path storageDir;

  @TempDir static Path tempDir;

  @MockitoBean private HttpClient httpClient;

  @MockitoBean HttpResponse<String> lastReleaseResponse;

  @DynamicPropertySource
  static void storageProperties(DynamicPropertyRegistry registry) {
    registry.add("storage.root-dir", () -> storageDir.toString());
  }

  @Autowired MockMvc mockMvc;

  @TestBean(methodName = "jarHomeOverride")
  String jarHome;

  private static String jarHomeOverride() {
    return tempDir.toAbsolutePath().toString();
  }

  @TestBean(methodName = "rebootScriptRunnerOverride")
  RebootScriptRunner rebootScriptRunner;

  private static RebootScriptRunner rebootScriptRunnerOverride() {
    return new RecordingRebootScriptRunner();
  }

  @TestBean(methodName = "configFileOverride")
  ConfigFile configFile;

  private static ConfigFile configFileOverride() {
    return new InMemoryConfigFile();
  }

  @BeforeEach
  void setUp() throws IOException {
    ((RecordingRebootScriptRunner) rebootScriptRunner).runconfigs.clear();

    try (Stream<Path> walk = Files.walk(tempDir)) {
      List<Path> files = walk.filter(not(tempDir::equals)).toList();
      for (Path file : files) {
        Files.delete(file);
      }
    }
  }

  @Test
  void getOidcConfig_GET() throws Exception {
    mockMvc
        .perform(get("/manage/auth/oidc-config").with(httpBasic("admin", "password")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.issuerUri").value("http://auth-server"))
        .andExpect(jsonPath("$.clientId").value("client-id"))
        .andExpect(jsonPath("$.clientSecret").value("this-is-very-secret"))
        .andExpect(jsonPath("$.deviceClientId").value("device-client-id"))
        .andExpect(jsonPath("$.deviceIssuerUri").doesNotExist());

    assertAuditEventPublished(GET_OIDC_CONFIG);
  }

  @Test
  @WithMockUser(roles = "USER")
  void getOidcConfig_GET_forbidden_for_non_su() throws Exception {
    mockMvc.perform(get("/manage/auth/oidc-config").with(csrf())).andExpect(status().isForbidden());
  }

  @Test
  void getOidcConfig_GET_unauthenticated() throws Exception {
    mockMvc.perform(get("/manage/auth/oidc-config")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "USER")
  void softRestart_POST_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/soft").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void softRestart_POST_unauthenticated() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/soft").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void hardRestart_POST() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/hard").with(csrf()).with(httpBasic("admin", "password")))
        .andExpect(status().isOk());

    assertRebootScriptCalledWithArgs("", "-c /etc/armadillo");
    assertAuditEventPublished(TRIGGER_HARD_RESTART);
  }

  @Test
  @WithMockUser(roles = "USER")
  void hardRestart_POST_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/hard").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void hardRestart_POST_unauthenticated() throws Exception {
    mockMvc
        .perform(post("/manage/app/restart/hard").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void update_POST() throws Exception {
    mockMvc
        .perform(
            post("/manage/app/update")
                .param("version", "1.2.3")
                .with(csrf())
                .with(httpBasic("admin", "password")))
        .andExpect(status().isOk());

    assertRebootScriptCalledWithArgs("1.2.3", "-u");
    assertAuditEventPublished(UPDATE_ARMADILLO, Map.of("ARMADILLO_VERSION", "1.2.3"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void update_POST_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(post("/manage/app/update").param("version", "1.2.3").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void update_POST_unauthenticated() throws Exception {
    mockMvc
        .perform(post("/manage/app/update").param("version", "1.2.3").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void listAvailable_GET() throws Exception {
    createFile("molgenis-armadillo-1.0.0.jar");
    createFile("molgenis-armadillo-4.2.0.jar");
    createFile("random.txt");

    MvcResult result =
        mockMvc
            .perform(get("/manage/app/list").with(csrf()).with(httpBasic("admin", "password")))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals(
        "[\"molgenis-armadillo-1.0.0.jar\",\"molgenis-armadillo-4.2.0.jar\"]",
        result.getResponse().getContentAsString());

    assertAuditEventPublished(LIST_AVAILABLE_VERSIONS);
  }

  @Test
  @WithMockUser(roles = "USER")
  void listAvailable_GET_forbidden_for_non_su() throws Exception {
    mockMvc.perform(get("/manage/app/list").with(csrf())).andExpect(status().isForbidden());
  }

  @Test
  void listAvailable_GET_unauthenticated() throws Exception {
    mockMvc.perform(get("/manage/app/list").with(csrf())).andExpect(status().isUnauthorized());
  }

  private static File createFile(String fileName) throws IOException {
    File file = tempDir.resolve(fileName).toFile();
    if (!file.createNewFile()) {
      fail("Unable to create " + fileName);
    }

    assertTrue(file.exists());
    return file;
  }

  @Test
  void deleteJar_DELETE() throws Exception {
    File file = createFile("molgenis-armadillo-1.0.0.jar");
    mockMvc
        .perform(
            delete("/manage/app/delete-jar")
                .param("version", "1.0.0")
                .with(csrf())
                .with(httpBasic("admin", "password")))
        .andExpect(status().isOk());

    assertFalse(file.exists());
    assertAuditEventPublished(DELETE_JAR, Map.of("VERSION_TO_DELETE", "1.0.0"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void deleteJar_DELETE_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(delete("/manage/app/delete-jar").param("version", "1.0.0").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void deleteJar_DELETE_unauthenticated() throws Exception {
    mockMvc
        .perform(delete("/manage/app/delete-jar").param("version", "1.0.0").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void getLastReleaseInfo_GET() throws Exception {
    when(httpClient.<String>send(any(), any())).thenReturn(lastReleaseResponse);
    when(lastReleaseResponse.statusCode()).thenReturn(200);
    when(lastReleaseResponse.body()).thenReturn("{\"tag_name\":\"v1.2.3\"}");
    MvcResult result =
        mockMvc
            .perform(
                get("/manage/app/latest-release-info")
                    .with(csrf())
                    .with(httpBasic("admin", "password")))
            .andExpect(status().isOk())
            .andReturn();

    assertEquals("{\"tag_name\":\"v1.2.3\"}", result.getResponse().getContentAsString());

    assertAuditEventPublished(GET_RELEASE_VERSION);
  }

  @Test
  @WithMockUser(roles = "USER")
  void getLastReleaseInfo_GET_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(get("/manage/app/latest-release-info").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void getLastReleaseInfo_GET_unauthenticated() throws Exception {
    mockMvc
        .perform(get("/manage/app/latest-release-info").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void downloadUpdateScript_POST() throws Exception {
    try (MockedStatic<FileDownloader> downloader = Mockito.mockStatic(FileDownloader.class)) {
      downloader
          .when(() -> FileDownloader.downloadFile(anyString(), anyString()))
          .thenAnswer(
              interceptor -> {
                System.out.println();
                Path path = tempDir.resolve("armadillo-reboot.sh");
                Files.write(path, "Hello world".getBytes());
                return null;
              });

      mockMvc
          .perform(
              post("/manage/updater/download")
                  .param("armadilloVersion", "1.2.3")
                  .with(csrf())
                  .with(httpBasic("admin", "password")))
          .andExpect(status().isCreated());

      String actual = Files.readString(Path.of(tempDir.resolve("armadillo-reboot.sh").toString()));
      assertEquals("Hello world", actual);
    }

    assertAuditEventPublished(DOWNLOAD_UPDATE_SCRIPT, Map.of("ARMADILLO_VERSION", "1.2.3"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void downloadUpdateScript_POST_forbidden_for_non_su() throws Exception {
    mockMvc
        .perform(post("/manage/updater/download").param("armadilloVersion", "1.2.3").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void downloadUpdateScript_POST_unauthenticated() throws Exception {
    mockMvc
        .perform(post("/manage/updater/download").param("armadilloVersion", "1.2.3").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void projectsUpsert_PUT() throws Exception {
    OidcDetails expected =
        OidcDetails.create(
            "https://issuer.example.com",
            "my-client-id",
            "my-client-secret",
            "https://issuer.example.com",
            "my-client-id");
    mockMvc
        .perform(
            put("/manage/auth/oidc-config")
                .content(GSON.toJson(expected))
                .contentType(APPLICATION_JSON)
                .with(csrf())
                .with(httpBasic("admin", "password")))
        .andExpect(status().isNoContent());

    assertRebootScriptCalledWithArgs("", "-c /etc/armadillo");
    assertEquals(expected, ((InMemoryConfigFile) configFile).getOidcDetails());
    assertAuditEventPublished(UPDATE_OIDC_CONFIG, Map.of("OIDC_DETAILS", expected));
  }

  @Test
  @WithMockUser(roles = "USER")
  void projectsUpsert_PUT_forbidden_for_non_su() throws Exception {
    var oidcDetails =
        OidcDetails.create(
            "https://issuer.example.com",
            "my-client-id",
            "my-client-secret",
            "https://issuer.example.com",
            "my-client-id");

    mockMvc
        .perform(
            put("/manage/auth/oidc-config")
                .content(new Gson().toJson(oidcDetails))
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void projectsUpsert_PUT_unauthenticated() throws Exception {
    var oidcDetails =
        OidcDetails.create(
            "https://issuer.example.com",
            "my-client-id",
            "my-client-secret",
            "https://issuer.example.com",
            "my-client-id");

    mockMvc
        .perform(
            put("/manage/auth/oidc-config")
                .content(new Gson().toJson(oidcDetails))
                .contentType(APPLICATION_JSON)
                .with(csrf()))
        .andExpect(
            result -> {
              result.equals("{\"attemptsRemaining\": 4}");
            });
  }

  private void assertRebootScriptCalledWithArgs(String version, String extraArg) {
    String script = tempDir.resolve("armadillo-reboot.sh").toString();
    String javaProcessId = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    String expected =
        script
            + " -p "
            + armadilloHome
            + " -v "
            + version
            + " -m PROD -i "
            + javaProcessId
            + " "
            + extraArg;
    await()
        .atMost(5, SECONDS)
        .until(
            () ->
                ((RecordingRebootScriptRunner) rebootScriptRunner)
                    .getRecordedRuns()
                    .equals(List.of(expected)));
  }

  private void assertAuditEventPublished(String eventName) {
    try {
      verify(auditor).audit(any(Runnable.class), any(Principal.class), eq(eventName));
    } catch (AssertionError e1) {
      try {
        verify(auditor).audit(any(Supplier.class), any(Principal.class), eq(eventName));
      } catch (AssertionError e2) {
        throw new AssertionError(
            "Neither Runnable nor Supplier audit() call matched with eventName=" + eventName, e2);
      }
    }
  }

  private void assertAuditEventPublished(String eventName, Map<String, Object> data) {
    try {
      verify(auditor).audit(any(Runnable.class), any(Principal.class), eq(eventName), eq(data));
    } catch (AssertionError e1) {
      try {
        verify(auditor).audit(any(Supplier.class), any(Principal.class), eq(eventName), eq(data));
      } catch (AssertionError e2) {
        throw new AssertionError(
            "Neither Runnable nor Supplier audit() call matched with eventName=" + eventName, e2);
      }
    }
  }

  static class RecordingRebootScriptRunner implements RebootScriptRunner {

    private final List<String> runconfigs = new ArrayList<>();

    @Override
    public void runRebootScript(String... args) {
      runconfigs.add(String.join(" ", args));
    }

    public List<String> getRecordedRuns() {
      return runconfigs;
    }
  }

  static class InMemoryConfigFile implements ConfigFile {

    private OidcDetails oidcDetails;

    @Override
    public void update(OidcDetails oidcDetails) {
      this.oidcDetails = oidcDetails;
    }

    @Override
    public Map<String, Object> getConfig() {
      return Map.of();
    }

    public OidcDetails getOidcDetails() {
      return oidcDetails;
    }
  }
}
