package org.molgenis.armadillo.service;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.molgenis.armadillo.TestHelpers.setField;

import com.google.gson.JsonElement;
import java.io.*;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.config.ApplicationConfigFile;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.molgenis.armadillo.storage.FileDownloader;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@ExtendWith(MockitoExtension.class)
class ManagementServiceTest {

  @Mock HttpClient httpClient;

  @Mock HttpResponse<String> lastReleaseResponse;

  ManagementService service;
  BuildProperties buildProperties;
  RecordingJarDownloader fakeDownloader;

  @TempDir Path tempDir;
  private String applicationConfigFile;

  GithubApi githubApi;
  UpdateScriptDownloader updateScriptDownloader;

  @BeforeEach
  void setUp() {
    buildProperties = mock(BuildProperties.class);
    applicationConfigFile = tempDir.resolve("application.yml").toString();
    fakeDownloader = new RecordingJarDownloader(tempDir.toString());
    githubApi = new GithubApi(httpClient);
    updateScriptDownloader = new UpdateScriptDownloader(tempDir.toString());
    FileService fileServiceMock = mock(FileService.class);
    service =
        new ManagementService(
            applicationConfigFile,
            "https://auth.example.com",
            "my-client",
            "secret123",
            "https://device.auth.example.com",
            "device-client",
            buildProperties,
            new DefaultRebootScriptRunner("./logs/armadillo.log"),
            fakeDownloader,
            githubApi,
            updateScriptDownloader,
            fileServiceMock,
            tempDir.toString(),
            new ApplicationConfigFile(applicationConfigFile));
  }

  @Test
  void downloadArmadilloJar_downloadsAndCompletes() throws Exception {
    SseEmitter emitter = service.downloadArmadilloJar("5.12.2");

    CountDownLatch completed = new CountDownLatch(1);
    AtomicReference<Throwable> error = new AtomicReference<>();
    emitter.onCompletion(completed::countDown);
    emitter.onError(error::set);

    await().atMost(5, SECONDS).until(() -> fakeDownloader.wasCalled());

    // Simulate what Spring MVC would do: fire completion once download logic finishes.
    // Since we're not going through initialize(), completion callbacks registered
    // via onCompletion() are stored and won't fire automatically outside a real
    // dispatch — so instead just assert on the concrete outcome:
    Path jarPath = tempDir.resolve("molgenis-armadillo-5.12.2.jar");
    await().atMost(5, SECONDS).until(() -> Files.exists(jarPath));
    assertEquals("Hello world!!", Files.readString(jarPath));
    assertNull(error.get());
  }

  @Test
  void getCurrentOidcConfig_returnsAllFields() {
    var config = service.getCurrentOidcConfig();
    assertEquals("https://auth.example.com", config.get("issuerUri"));
    assertEquals("my-client", config.get("clientId"));
    assertEquals("secret123", config.get("clientSecret"));
    assertEquals("device-client", config.get("deviceClientId"));
    assertEquals("https://device.auth.example.com", config.get("deviceIssuerUri"));
  }

  @Test
  void deleteJar_throwsWhenDeletingRunningVersion() {
    when(buildProperties.getVersion()).thenReturn("5.14.0");
    assertThrows(StorageException.class, () -> service.deleteJar("5.14.0"));
  }

  @Test
  void deleteJar_deletesJarWhenNotRunning() throws Exception {
    when(buildProperties.getVersion()).thenReturn("5.14.0"); // needed: must not equal "5.13.0"
    Path jar = tempDir.resolve("molgenis-armadillo-5.13.0.jar");
    Files.createFile(jar);

    service.deleteJar("5.13.0");

    assertFalse(Files.exists(jar));
  }

  @Test
  void deleteJar_throwsWhenFileDoesNotExist() {
    when(buildProperties.getVersion()).thenReturn("5.14.0");
    assertThrows(StorageException.class, () -> service.deleteJar("1.0.0"));
  }

  @Test
  void listLocallyAvailableJars_returnsOnlyJarFiles() throws Exception {
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.14.0.jar"));
    Files.createFile(tempDir.resolve("armadillo-reboot.sh"));
    Files.createFile(tempDir.resolve("application.yml"));

    var jars = service.listLocallyAvailableJars();

    assertEquals(1, jars.size());
    assertTrue(jars.contains("molgenis-armadillo-5.14.0.jar"));
  }

  @Test
  void listLocallyAvailableJars_returnsEmptySetWhenNoJars() throws Exception {
    Files.createFile(tempDir.resolve("some-other-file.txt"));

    var jars = service.listLocallyAvailableJars();

    assertTrue(jars.isEmpty());
  }

  @Test
  void listLocallyAvailableJars_returnsMultipleJars() throws Exception {
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.14.0.jar"));
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.13.0.jar"));

    var jars = service.listLocallyAvailableJars();

    assertEquals(2, jars.size());
  }

  @Test
  void getJarFromVersion_stripsVPrefix() throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("getJarFromVersion", String.class);
    m.setAccessible(true);

    String jar = (String) m.invoke(service, "v5.14.0");
    assertEquals("molgenis-armadillo-5.14.0.jar", jar);
  }

  @Test
  void getJarFromVersion_worksWithoutVPrefix() throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("getJarFromVersion", String.class);
    m.setAccessible(true);

    String jar = (String) m.invoke(service, "5.14.0");
    assertEquals("molgenis-armadillo-5.14.0.jar", jar);
  }

  @Test
  void getJarPathFromVersion_composesCorrectPath() throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("getJarPathFromVersion", String.class);
    m.setAccessible(true);

    String path = (String) m.invoke(service, "5.14.0");
    assertEquals(tempDir + File.separator + "molgenis-armadillo-5.14.0.jar", path);
  }

  @Test
  void listLocallyAvailableJars_excludesSubdirectories() throws Exception {
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.14.0.jar"));
    Files.createDirectory(tempDir.resolve("subdir.jar")); // a directory named like a jar

    var jars = service.listLocallyAvailableJars();

    assertEquals(1, jars.size());
    assertTrue(jars.contains("molgenis-armadillo-5.14.0.jar"));
    assertFalse(jars.contains("subdir.jar"));
  }

  @Test
  void downloadUpdateScript_FailsWhenInvalidVersion() {
    assertThrows(ResponseStatusException.class, () -> service.downloadUpdateScript("INVALID"));
  }

  @Test
  void downloadUpdateScript_FailsWhenRebootScriptMissing() {
    AtomicBoolean called = new AtomicBoolean(false);
    try (MockedStatic<FileDownloader> downloader = Mockito.mockStatic(FileDownloader.class)) {
      downloader
          .when(() -> FileDownloader.downloadFile(anyString(), anyString()))
          .thenAnswer(
              interceptor -> {
                called.set(true);
                return null;
              });

      assertThrows(ResponseStatusException.class, () -> service.downloadUpdateScript("v1.1.0"));
    }
  }

  @Test
  void downloadUpdateScript_TriggersDownloadMethod() throws Exception {
    AtomicBoolean called = new AtomicBoolean(false);
    Files.createFile(tempDir.resolve("armadillo-reboot.sh"));
    try (MockedStatic<FileDownloader> downloader = Mockito.mockStatic(FileDownloader.class)) {
      downloader
          .when(() -> FileDownloader.downloadFile(anyString(), anyString()))
          .thenAnswer(
              interceptor -> {
                called.set(true);
                return null;
              });

      service.downloadUpdateScript("v1.1.0");
      assertTrue(called.get());
    }
  }

  @Test
  void getLastRelease_should_return_json_on_200() throws Exception {
    when(httpClient.<String>send(any(), any())).thenReturn(lastReleaseResponse);
    when(lastReleaseResponse.statusCode()).thenReturn(200);
    when(lastReleaseResponse.body()).thenReturn("{\"tag_name\":\"v1.0\"}");

    JsonElement result = service.getLastRelease();

    assertThat(result.getAsJsonObject().get("tag_name").getAsString()).isEqualTo("v1.0");
  }

  @Test
  void getLastRelease_should_throw_on_non_200() throws Exception {
    when(httpClient.<String>send(any(), any())).thenReturn(lastReleaseResponse);
    when(lastReleaseResponse.statusCode()).thenReturn(404);

    assertThatThrownBy(() -> service.getLastRelease()).isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void hardRestartApplication_throw_error_when_in_docker() throws Exception {
    setField(service, "runningInContainer", true);
    assertThrows(UnsupportedOperationException.class, () -> service.hardRestartApplication());
  }

  @Test
  void throwWhenRunningInContainer_does_not_throw_error_when_not_in_docker() {
    assertDoesNotThrow(() -> service.throwWhenRunningInContainer("method"));
  }

  @Test
  void throwWhenRunningInContainer_does_not_throw_error_when_not_in_production() {
    assertDoesNotThrow(() -> service.throwWhenNotLinuxInProd("method"));
  }

  @Test
  void saveNewOidcConfig_throw_error_when_in_docker() throws Exception {
    setField(service, "runningInContainer", true);
    OidcDetails oidcDetails = mock(OidcDetails.class);
    assertThrows(UnsupportedOperationException.class, () -> service.saveNewOidcConfig(oidcDetails));
  }

  @Test
  void triggerUpdate_throw_error_when_in_docker() throws Exception {
    setField(service, "runningInContainer", true);
    assertThrows(UnsupportedOperationException.class, () -> service.triggerUpdate("x.y.z"));
  }

  @Test
  void getUpdateScriptPath() {
    String updateScriptPath = service.getUpdateScriptPath();
    assertThat(updateScriptPath).isEqualTo(tempDir + "/armadillo-reboot.sh");
  }

  @Test
  void getUpdateScriptUrl() {
    String updateScriptUrl = service.getUpdateScriptUrl("v5.0.1");
    assertThat(updateScriptUrl)
        .isEqualTo(
            "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/70429b0a4ebdb579fdfd66df9bacae3a67866135/scripts/install/armadillo-reboot.sh");
  }

  @Test
  void getUpdateScriptUrlAfterMerge() {
    String updateScriptUrl = service.getUpdateScriptUrl("v5.17.1");
    assertThat(updateScriptUrl)
        .isEqualTo(
            "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/refs/tags/v5.17.1/scripts/install/armadillo-reboot.sh");
  }

  @Test
  void getUpdateScriptUrlDev() {
    String updateScriptUrl = service.getUpdateScriptUrl("dev");
    assertThat(updateScriptUrl)
        .isEqualTo(
            "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/70429b0a4ebdb579fdfd66df9bacae3a67866135/scripts/install/armadillo-reboot.sh");
  }

  @Test
  void isValidVersion() {
    assertTrue(service.isValidVersion("v1.3.1"));
    assertTrue(service.isValidVersion("v1341.331.12"));
    assertTrue(service.isValidVersion("13.31.121234"));
    assertTrue(service.isValidVersion("v5.14.0-SNAPSHOT"));
    assertTrue(service.isValidVersion("6.1.0-SNAPSHOT"));
    assertTrue(service.isValidVersion("dev"));
    assertFalse(service.isValidVersion("v1.3a.31a"));
    assertFalse(service.isValidVersion("print('do something very evil?')"));
  }

  @Test
  void downloadArmadilloJar_FailsWhenInvalidVersion() {
    assertThrows(ResponseStatusException.class, () -> service.downloadArmadilloJar("INVALID"));
  }

  @Test
  void triggerUpdate_FailsWhenInvalidVersion() {
    assertThrows(ResponseStatusException.class, () -> service.triggerUpdate("INVALID"));
  }

  @Test
  void getJavaProcessId_returns_id() {
    String name = "52005@My-MacBook-Pro.local";
    assertEquals("52005", service.getJavaProcessId(name));
  }
}
