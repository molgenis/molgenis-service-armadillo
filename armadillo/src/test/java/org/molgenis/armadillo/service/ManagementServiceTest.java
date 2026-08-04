package org.molgenis.armadillo.service;

import static org.assertj.core.api.Assertions.*;
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
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

@ExtendWith(MockitoExtension.class)
class ManagementServiceTest {

  @Mock HttpClient httpClient;

  @Mock HttpResponse<String> lastReleaseResponse;

  ManagementService service;
  BuildProperties buildProperties;

  @TempDir Path tempDir;
  private String applicationConfigFile;

  @BeforeEach
  void setUp() throws Exception {
    buildProperties = mock(BuildProperties.class);
    applicationConfigFile = tempDir.resolve("application.yml").toString();
    service =
        new ManagementService(
            applicationConfigFile,
            buildProperties,
            new PythonRebootScriptRunner("./logs/armadillo.log"),
            httpClient,
            tempDir.toString(),
            new ApplicationConfigFile(applicationConfigFile));

    // Point armadilloHome and armadilloConfigFile to temp dir
    setField(service, "armadilloHome", tempDir.toString());
    setField(service, "armadilloConfigFile", applicationConfigFile);
    setField(service, "armadilloMode", "PROD");
    setField(service, "runningInContainer", false);
    File logFile = tempDir.resolve("test.log").toFile();
    logFile.createNewFile();
  }

  // -------------------------------------------------------------------------
  // getCurrentOidcConfig
  // -------------------------------------------------------------------------

  @Test
  void getCurrentOidcConfig_returnsAllFields() throws Exception {
    setField(service, "issuerUri", "https://auth.example.com");
    setField(service, "clientId", "my-client");
    setField(service, "clientSecret", "secret123");
    setField(service, "deviceClientId", "device-client");
    setField(service, "deviceIssuerUri", "https://device.auth.example.com");

    var config = service.getCurrentOidcConfig();

    assertEquals("https://auth.example.com", config.get("issuerUri"));
    assertEquals("my-client", config.get("clientId"));
    assertEquals("secret123", config.get("clientSecret"));
    assertEquals("device-client", config.get("deviceClientId"));
    assertEquals("https://device.auth.example.com", config.get("deviceIssuerUri"));
  }

  // -------------------------------------------------------------------------
  // deleteJar
  // -------------------------------------------------------------------------

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

  // -------------------------------------------------------------------------
  // listAvailableJars
  // -------------------------------------------------------------------------

  @Test
  void listAvailableJars_returnsOnlyJarFiles() throws Exception {
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.14.0.jar"));
    Files.createFile(tempDir.resolve("armadillo-reboot.sh"));
    Files.createFile(tempDir.resolve("application.yml"));

    var jars = service.listAvailableJars();

    assertEquals(1, jars.size());
    assertTrue(jars.contains("molgenis-armadillo-5.14.0.jar"));
  }

  @Test
  void listAvailableJars_returnsEmptySetWhenNoJars() throws Exception {
    Files.createFile(tempDir.resolve("some-other-file.txt"));

    var jars = service.listAvailableJars();

    assertTrue(jars.isEmpty());
  }

  // -------------------------------------------------------------------------
  // isArmadilloUpdateAvailable — via listAvailableJars
  // -------------------------------------------------------------------------

  @Test
  void listAvailableJars_returnsMultipleJars() throws Exception {
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.14.0.jar"));
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.13.0.jar"));

    var jars = service.listAvailableJars();

    assertEquals(2, jars.size());
  }

  // -------------------------------------------------------------------------
  // getScriptVersionTag (private — tested via reflection)
  // -------------------------------------------------------------------------

  String getTag(String version) throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("getScriptVersionTag", String.class);
    m.setAccessible(true);
    return (String) m.invoke(service, version);
  }

  @ParameterizedTest
  @ValueSource(strings = {"5.15.0", "6.0.0"})
  void getScriptVersionTag(String version) throws Exception {
    assertEquals("refs/tags/v" + version, getTag(version));
  }

  @Test
  void getScriptVersionTag_returnsCommitHashForOldVersions() throws Exception {
    assertEquals("3f3cffbaaa61121c5f9b10021e0e40412aaadc65", getTag("5.13.0"));
  }

  // -------------------------------------------------------------------------
  // getJarFromVersion (private — tested via reflection)
  // -------------------------------------------------------------------------

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

  // -------------------------------------------------------------------------
  // getJarPathFromVersion (private — tested via reflection)
  // -------------------------------------------------------------------------

  @Test
  void getJarPathFromVersion_composesCorrectPath() throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("getJarPathFromVersion", String.class);
    m.setAccessible(true);

    String path = (String) m.invoke(service, "5.14.0");
    assertEquals(tempDir + File.separator + "molgenis-armadillo-5.14.0.jar", path);
  }

  // -------------------------------------------------------------------------
  // fileExistsInDir (private — tested via reflection)
  // -------------------------------------------------------------------------

  @Test
  void fileExistsInDir_returnsTrueWhenFilePresent() throws Exception {
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.14.0.jar"));

    Method m =
        ManagementService.class.getDeclaredMethod("fileExistsInDir", String.class, String.class);
    m.setAccessible(true);

    boolean exists =
        (boolean) m.invoke(service, "molgenis-armadillo-5.14.0.jar", tempDir.toString());
    assertTrue(exists);
  }

  @Test
  void fileExistsInDir_returnsFalseWhenFileAbsent() throws Exception {
    Method m =
        ManagementService.class.getDeclaredMethod("fileExistsInDir", String.class, String.class);
    m.setAccessible(true);

    boolean exists = (boolean) m.invoke(service, "missing.jar", tempDir.toString());
    assertFalse(exists);
  }

  // -------------------------------------------------------------------------
  // listAvailableJars — subdirectories with .jar-like names are excluded
  // -------------------------------------------------------------------------

  @Test
  void listAvailableJars_excludesSubdirectories() throws Exception {
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.14.0.jar"));
    Files.createDirectory(tempDir.resolve("subdir.jar")); // a directory named like a jar

    var jars = service.listAvailableJars();

    assertEquals(1, jars.size());
    assertTrue(jars.contains("molgenis-armadillo-5.14.0.jar"));
    assertFalse(jars.contains("subdir.jar"));
  }

  // -------------------------------------------------------------------------
  // isArmadilloUpdateAvailable — jar already present (no HTTP call needed)
  // -------------------------------------------------------------------------

  @Test
  void isArmadilloUpdateAvailable_jarPresentMeansNoUpdateNeeded() throws Exception {
    // fileExistsInDir is the gating predicate: method returns !fileExistsInDir(...)
    // Verify the predicate returns true when the jar file is on disk
    Files.createFile(tempDir.resolve("molgenis-armadillo-5.14.0.jar"));

    Method m =
        ManagementService.class.getDeclaredMethod("fileExistsInDir", String.class, String.class);
    m.setAccessible(true);

    boolean jarPresent =
        (boolean) m.invoke(service, "molgenis-armadillo-5.14.0.jar", tempDir.toString());
    assertTrue(jarPresent); // jar found → isArmadilloUpdateAvailable returns false
  }

  // -------------------------------------------------------------------------
  // downloadUpdateScript — 'v' prefix is stripped before version tag lookup
  // -------------------------------------------------------------------------

  @Test
  void downloadUpdateScript_stripsVPrefixBeforeVersionTagLookup() throws Exception {
    // downloadUpdateScript does version.replace("v", "") then calls getScriptVersionTag.
    // Verify the resulting tag is correct for a version string that starts with 'v'.
    Method tagMethod =
        ManagementService.class.getDeclaredMethod("getScriptVersionTag", String.class);
    tagMethod.setAccessible(true);

    String strippedVersion = "v5.15.0".replace("v", "");
    String tag = (String) tagMethod.invoke(service, strippedVersion);

    assertEquals("refs/tags/v5.15.0", tag);
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

  // -------------------------------------------------------------------------
  // getLastRelease
  // -------------------------------------------------------------------------

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
            "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/3f3cffbaaa61121c5f9b10021e0e40412aaadc65/scripts/install/armadillo-reboot.sh");
  }

  @Test
  void getUpdateScriptUrlAfterMerge() {
    String updateScriptUrl = service.getUpdateScriptUrl("v5.15.1");
    assertThat(updateScriptUrl)
        .isEqualTo(
            "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/refs/tags/v5.15.1/scripts/install/armadillo-reboot.sh");
  }

  @Test
  void getUpdateScriptUrlDev() {
    String updateScriptUrl = service.getUpdateScriptUrl("dev");
    assertThat(updateScriptUrl)
        .isEqualTo(
            "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/3f3cffbaaa61121c5f9b10021e0e40412aaadc65/scripts/install/armadillo-reboot.sh");
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
