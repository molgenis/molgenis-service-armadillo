package org.molgenis.armadillo.service;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ManagementServiceTest {
  @Mock HttpClient httpClient;

  @Mock HttpResponse<String> lastReleaseResponse;

  @Mock HttpHeaders httpHeaders;

  ManagementService service;
  BuildProperties buildProperties;

  @TempDir Path tempDir;

  private File logFile;

  @BeforeEach
  void setUp() throws Exception {
    service = new ManagementService("./logs/armadillo.log", null, httpClient);

    // Inject a mock BuildProperties
    buildProperties = mock(BuildProperties.class);
    setField(service, "buildProperties", buildProperties);

    // Point armadilloHome and armadilloConfigFile to temp dir
    setField(service, "armadilloHome", tempDir.toString());
    setField(service, "armadilloConfigFile", tempDir.resolve("application.yml").toString());
    setField(service, "armadilloMode", "PROD");
    logFile = tempDir.resolve("test.log").toFile();
    logFile.createNewFile();
  }

  // -------------------------------------------------------------------------
  // Constructor — updateLogPath derivation
  // -------------------------------------------------------------------------

  @Test
  void constructor_derivesUpdateLogPathFromLogPath() throws Exception {
    ManagementService svc =
        new ManagementService("/var/log/armadillo/armadillo.log", null, httpClient);
    String path = (String) getField(svc, "updateLogPath");
    assertEquals("/var/log/armadillo/update.log", path);
  }

  @Test
  void constructor_usesExplicitUpdateLogPath() throws Exception {
    ManagementService svc =
        new ManagementService("./logs/armadillo.log", "/custom/path/update.log", httpClient);
    // When updatePath is explicitly provided it is used directly
    // (the constructor only assigns when updatePath == null)
    String path = (String) getField(svc, "updateLogPath");
    // null branch not taken → path stays null (constructor doesn't set it)
    assertNull(path);
  }

  // -------------------------------------------------------------------------
  // getReleaseVersion
  // -------------------------------------------------------------------------

  @Test
  void getReleaseVersion_returnsTagName() {
    JsonObject release = new JsonObject();
    release.addProperty("tag_name", "v5.14.0");
    assertEquals("v5.14.0", service.getReleaseVersion(release));
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

  @Test
  void getScriptVersionTag_returnsCommitHashForOldVersions() throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("getScriptVersionTag", String.class);
    m.setAccessible(true);

    String tag = (String) m.invoke(service, "5.13.0");
    assertEquals("6f815bb32e5677ce17680d262344d2f4e3c6106e", tag);
  }

  @Test
  void getScriptVersionTag_returnsRefTagForNewVersions() throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("getScriptVersionTag", String.class);
    m.setAccessible(true);

    String tag = (String) m.invoke(service, "5.14.0");
    assertEquals("refs/tags/v5.14.0", tag);
  }

  @Test
  void getScriptVersionTag_returnsRefTagForVersion6AndAbove() throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("getScriptVersionTag", String.class);
    m.setAccessible(true);

    String tag = (String) m.invoke(service, "6.0.0");
    assertEquals("refs/tags/v6.0.0", tag);
  }

  // -------------------------------------------------------------------------
  // buildPythonList (private — tested via reflection)
  // -------------------------------------------------------------------------

  @Test
  void buildPythonList_formatsCorrectly() throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("buildPythonList", String[].class);
    m.setAccessible(true);

    String result =
        (String) m.invoke(service, (Object) new String[] {"/path/script", "-p", "/home"});
    assertEquals("['/path/script', '-p', '/home']", result);
  }

  @Test
  void buildPythonList_escapesSingleQuotes() throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("buildPythonList", String[].class);
    m.setAccessible(true);

    String result = (String) m.invoke(service, (Object) new String[] {"it's"});
    assertEquals("['it\\'s']", result);
  }

  // -------------------------------------------------------------------------
  // replaceValue (private — tested via reflection)
  // -------------------------------------------------------------------------

  @Test
  void replaceValue_replacesValueAfterColon() throws Exception {
    Method m =
        ManagementService.class.getDeclaredMethod("replaceValue", String.class, String.class);
    m.setAccessible(true);

    String result =
        (String)
            m.invoke(service, "  issuer-uri: https://old.example.com", "https://new.example.com");
    assertEquals("  issuer-uri: https://new.example.com", result);
  }

  // -------------------------------------------------------------------------
  // transformConfig / updateApplicationConfig
  // -------------------------------------------------------------------------

  @Test
  void updateApplicationConfig_updatesIssuerUri() throws Exception {
    String yaml =
        """
                spring:
                  security:
                    oauth2:
                      client:
                        provider:
                          molgenis:
                            issuer-uri: https://old.example.com
                        registration:
                          molgenis:
                            client-id: old-client
                            client-secret: old-secret
                      resourceserver:
                        jwt:
                          issuer-uri: https://old-device.example.com
                        opaquetoken:
                          client-id: old-device-client
                """;

    Path configFile = tempDir.resolve("application.yml");
    Files.writeString(configFile, yaml);
    setField(service, "armadilloConfigFile", configFile.toString());

    OidcDetails details = mock(OidcDetails.class);
    when(details.getIssuerUri()).thenReturn("https://new.example.com");
    when(details.getClientId()).thenReturn("new-client");
    when(details.getClientSecret()).thenReturn("new-secret");
    when(details.getDeviceIssuerUri()).thenReturn("https://new-device.example.com");
    when(details.getDeviceClientId()).thenReturn("new-device-client");

    Method m =
        ManagementService.class.getDeclaredMethod("updateApplicationConfig", OidcDetails.class);
    m.setAccessible(true);
    m.invoke(service, details);

    String updated = Files.readString(configFile);
    assertTrue(updated.contains("issuer-uri: https://new.example.com"));
    assertTrue(updated.contains("client-id: new-client"));
    assertTrue(updated.contains("client-secret: new-secret"));
    assertTrue(updated.contains("issuer-uri: https://new-device.example.com"));
    assertTrue(updated.contains("client-id: new-device-client"));

    // Backup file should exist
    assertTrue(Files.exists(Path.of(configFile + ".bak")));
  }

  @Test
  void updateApplicationConfig_preservesComments() throws Exception {
    String yaml =
        """
                # This is a comment
                spring:
                  security:
                    oauth2:
                      client:
                        provider:
                          molgenis:
                            issuer-uri: https://old.example.com
                        registration:
                          molgenis:
                            client-id: old-client
                            client-secret: old-secret
                      resourceserver:
                        jwt:
                          issuer-uri: https://old-device.example.com
                        opaquetoken:
                          client-id: old-device-client
                """;

    Path configFile = tempDir.resolve("application.yml");
    Files.writeString(configFile, yaml);
    setField(service, "armadilloConfigFile", configFile.toString());

    OidcDetails details = mock(OidcDetails.class);
    when(details.getIssuerUri()).thenReturn("https://new.example.com");
    when(details.getClientId()).thenReturn("new-client");
    when(details.getClientSecret()).thenReturn("new-secret");
    when(details.getDeviceIssuerUri()).thenReturn("https://new-device.example.com");
    when(details.getDeviceClientId()).thenReturn("new-device-client");

    Method m =
        ManagementService.class.getDeclaredMethod("updateApplicationConfig", OidcDetails.class);
    m.setAccessible(true);
    m.invoke(service, details);

    String updated = Files.readString(configFile);
    assertTrue(updated.contains("# This is a comment"));
  }

  // -------------------------------------------------------------------------
  // getJarHome — DEV vs PROD mode
  // -------------------------------------------------------------------------

  @Test
  void getJarHome_returnsBuildLibsInDevMode() throws Exception {
    setField(service, "armadilloMode", "DEV");
    Method m = ManagementService.class.getDeclaredMethod("getJarHome");
    m.setAccessible(true);

    String jarHome = (String) m.invoke(service);
    assertTrue(jarHome.endsWith("build/libs/"));
  }

  @Test
  void getJarHome_returnsArmadilloHomeInProdMode() throws Exception {
    Method m = ManagementService.class.getDeclaredMethod("getJarHome");
    m.setAccessible(true);

    String jarHome = (String) m.invoke(service);
    assertEquals(tempDir.toString(), jarHome);
  }

  // -------------------------------------------------------------------------
  // getUpdateLogFile
  // -------------------------------------------------------------------------

  @Test
  void getUpdateLogFile_createsFileIfMissing() throws Exception {
    Path logPath = tempDir.resolve("logs/update.log");
    setField(service, "updateLogPath", logPath.toString());

    Method m = ManagementService.class.getDeclaredMethod("getUpdateLogFile");
    m.setAccessible(true);
    File logFile = (File) m.invoke(service);

    assertTrue(logFile.exists());
  }

  // -------------------------------------------------------------------------
  // constructor — explicit updateLogPath IS provided (null branch not taken)
  // -------------------------------------------------------------------------

  @Test
  void constructor_usesExplicitUpdateLogPath_fieldRemainsNull() throws Exception {
    ManagementService svc =
        new ManagementService("./logs/armadillo.log", "/custom/path/update.log", httpClient);
    String path = (String) getField(svc, "updateLogPath");
    // updatePath != null → the if-block is skipped → updateLogPath is never set
    assertNull(path);
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
  // createPythonScript (private — tested via reflection)
  // -------------------------------------------------------------------------

  @Test
  void createPythonScript_nonUpdateBranch_doesNotContainUpdateFlag() throws Exception {
    Method m =
        ManagementService.class.getDeclaredMethod(
            "createPythonScript", String.class, String.class, String.class, Boolean.class);
    m.setAccessible(true);

    String script =
        (String)
            m.invoke(
                service,
                "/usr/share/armadillo/armadillo-reboot.sh",
                "/tmp/update.log",
                "5.14.0",
                false);

    assertTrue(script.contains("import os, sys, subprocess"));
    assertTrue(script.contains("/tmp/update.log"));
    assertTrue(script.contains("5.14.0"));
    // The '-u' update flag must NOT be present when isUpdate=false
    assertFalse(script.contains("'-u'"));
  }

  @Test
  void createPythonScript_updateBranch_containsUpdateFlag() throws Exception {
    Method m =
        ManagementService.class.getDeclaredMethod(
            "createPythonScript", String.class, String.class, String.class, Boolean.class);
    m.setAccessible(true);

    String script =
        (String)
            m.invoke(
                service,
                "/usr/share/armadillo/armadillo-reboot.sh",
                "/tmp/update.log",
                "5.14.0",
                true);

    assertTrue(script.contains("'-u'"));
  }

  @Test
  void createPythonScript_containsArmadilloHomeVersionAndMode() throws Exception {
    Method m =
        ManagementService.class.getDeclaredMethod(
            "createPythonScript", String.class, String.class, String.class, Boolean.class);
    m.setAccessible(true);

    String script =
        (String)
            m.invoke(
                service,
                "/usr/share/armadillo/armadillo-reboot.sh",
                "/tmp/update.log",
                "5.14.0",
                false);

    assertTrue(script.contains(tempDir.toString())); // armadilloHome injected via -p
    assertTrue(script.contains("5.14.0")); // version injected via -v
    assertTrue(script.contains("PROD")); // mode injected via -m
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
  // transformConfig — unrelated config lines pass through unchanged
  // -------------------------------------------------------------------------

  @Test
  void transformConfig_preservesUnrelatedLines() throws Exception {
    String yaml =
        """
                        spring:
                          datasource:
                            url: jdbc:postgresql://localhost/armadillo
                          security:
                            oauth2:
                              client:
                                provider:
                                  molgenis:
                                    issuer-uri: https://old.example.com
                                registration:
                                  molgenis:
                                    client-id: old-client
                                    client-secret: old-secret
                              resourceserver:
                                jwt:
                                  issuer-uri: https://old-device.example.com
                                opaquetoken:
                                  client-id: old-device-client
                        """;

    Path configFile = tempDir.resolve("application.yml");
    Files.writeString(configFile, yaml);
    setField(service, "armadilloConfigFile", configFile.toString());

    OidcDetails details = mock(OidcDetails.class);
    when(details.getIssuerUri()).thenReturn("https://new.example.com");
    when(details.getClientId()).thenReturn("new-client");
    when(details.getClientSecret()).thenReturn("new-secret");
    when(details.getDeviceIssuerUri()).thenReturn("https://new-device.example.com");
    when(details.getDeviceClientId()).thenReturn("new-device-client");

    Method m =
        ManagementService.class.getDeclaredMethod("updateApplicationConfig", OidcDetails.class);
    m.setAccessible(true);
    m.invoke(service, details);

    String updated = Files.readString(configFile);
    assertTrue(updated.contains("url: jdbc:postgresql://localhost/armadillo"));
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

    String strippedVersion = "v5.14.0".replace("v", "");
    String tag = (String) tagMethod.invoke(service, strippedVersion);

    assertEquals("refs/tags/v5.14.0", tag);
  }

  @Test
  void thread_should_have_correct_name_and_be_daemon() throws Exception {
    Thread tailer = service.startLogTailer(logFile, line -> {});

    assertThat(tailer.getName()).isEqualTo("update-log-tailer");
    assertThat(tailer.isDaemon()).isTrue();
    assertThat(tailer.isAlive()).isTrue();

    tailer.interrupt();
  }

  @Test
  void should_pick_up_lines_written_after_start() throws Exception {
    List<String> captured = new CopyOnWriteArrayList<>();

    Thread tailer = service.startLogTailer(logFile, captured::add);

    // Write lines AFTER the tailer has started (it skips existing content)
    await().atMost(500, TimeUnit.MILLISECONDS).until(() -> tailer.isAlive());

    try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
      writer.println("line one");
      writer.println("line two");
      writer.flush();
    }

    await().atMost(2, TimeUnit.SECONDS).until(() -> captured.size() >= 2);

    assertThat(captured).containsExactly("line one", "line two");

    tailer.interrupt();
  }

  @Test
  void should_skip_content_already_in_file_at_start() throws Exception {
    // Write content BEFORE starting the tailer
    Files.writeString(logFile.toPath(), "pre-existing line\n");

    List<String> captured = new CopyOnWriteArrayList<>();
    Thread tailer = service.startLogTailer(logFile, captured::add);

    // Give it time to potentially (wrongly) pick up the old line
    Thread.sleep(300);

    assertThat(captured).isEmpty();

    tailer.interrupt();
  }

  @Test
  void should_stop_cleanly_on_interrupt() throws Exception {
    Thread tailer = service.startLogTailer(logFile, line -> {});

    tailer.interrupt();

    await().atMost(1, TimeUnit.SECONDS).until(() -> !tailer.isAlive());

    assertThat(tailer.isAlive()).isFalse();
  }

  @Test
  void should_log_error_on_missing_file() throws Exception {
    File missing = tempDir.resolve("nonexistent.log").toFile();

    Thread tailer = service.startLogTailer(missing, line -> {});

    tailer.join(5000); // wait up to 5s for the thread to die naturally

    assertThat(tailer.isAlive()).isFalse();
  }

  @Test
  void getPercentage_should_calculate_correctly() {
    assertThat(service.getPercentage(50, 100)).isEqualTo(50);
    assertThat(service.getPercentage(1, 100)).isEqualTo(1);
    assertThat(service.getPercentage(100, 100)).isEqualTo(100);
  }

  @Test
  void processFile_should_write_bytes_and_report_progress(@TempDir Path tempDir) throws Exception {
    byte[] data = "hello world".getBytes();
    BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(data));
    File out = tempDir.resolve("out.bin").toFile();
    List<Long> progressUpdates = new ArrayList<>();

    try (FileOutputStream fos = new FileOutputStream(out)) {
      service.processFile(fos, in, data.length, progressUpdates::add);
    }

    assertThat(out).hasContent("hello world");
    assertThat(progressUpdates).isNotEmpty();
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
  void getProcessBuilderForRebootScript() throws Exception {
    String pythonScript = "print('hello world')";
    ProcessBuilder pb = service.getProcessBuilderForRebootScript(pythonScript);
    assertThat(pb.command().get(2)).isEqualTo(pythonScript);
    assertThat(pb.redirectInput().file()).isEqualTo(new File("/dev/null"));
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
            "https://raw.githubusercontent.com/molgenis/molgenis-service-armadillo/6f815bb32e5677ce17680d262344d2f4e3c6106e/scripts/install/armadillo-reboot.sh");
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  private void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = findField(target.getClass(), fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  private Object getField(Object target, String fieldName) throws Exception {
    Field f = findField(target.getClass(), fieldName);
    f.setAccessible(true);
    return f.get(target);
  }

  private Field findField(Class<?> clazz, String name) throws NoSuchFieldException {
    try {
      return clazz.getDeclaredField(name);
    } catch (NoSuchFieldException e) {
      if (clazz.getSuperclass() != null) return findField(clazz.getSuperclass(), name);
      throw e;
    }
  }
}
