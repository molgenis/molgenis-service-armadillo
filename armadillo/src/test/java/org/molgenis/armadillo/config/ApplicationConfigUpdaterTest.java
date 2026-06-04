package org.molgenis.armadillo.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.armadillo.metadata.OidcDetails;

class ApplicationConfigUpdaterTest {

  private OidcDetails oidcDetails;
  private ApplicationConfigUpdater updater;

  @TempDir Path tempDir;

  @BeforeEach
  void setUp() {
    oidcDetails = mock(OidcDetails.class);
    when(oidcDetails.getIssuerUri()).thenReturn("https://new-issuer.example.com");
    when(oidcDetails.getDeviceIssuerUri()).thenReturn("https://new-device-issuer.example.com");
    when(oidcDetails.getClientId()).thenReturn("new-client-id");
    when(oidcDetails.getDeviceClientId()).thenReturn("new-device-client-id");
    when(oidcDetails.getClientSecret()).thenReturn("new-client-secret");

    updater = new ApplicationConfigUpdater("unused");
  }

  // -------------------------------------------------------------------------
  // replaceValue
  // -------------------------------------------------------------------------

  @Test
  void replaceValue_replacesEverythingAfterFirstColon() {
    String result =
        updater.replaceValue("    issuer-uri: https://old.example.com", "https://new.example.com");
    assertEquals("    issuer-uri: https://new.example.com", result);
  }

  @Test
  void replaceValue_worksWhenValueContainsColons() {
    // URLs have colons; only the YAML key colon should be used as split point
    String result =
        updater.replaceValue(
            "issuer-uri: https://old.example.com:8080/path", "https://replaced.com");
    assertEquals("issuer-uri: https://replaced.com", result);
  }

  // -------------------------------------------------------------------------
  // transformConfig — comment lines pass through untouched
  // -------------------------------------------------------------------------

  @Test
  void transformConfig_commentLinesAreNotTransformed() {
    List<String> lines =
        List.of(
            "# issuer-uri: https://should-not-change.com", "  # client-id: also-should-not-change");
    String result = updater.transformConfig(lines, oidcDetails);
    assertTrue(result.contains("# issuer-uri: https://should-not-change.com"));
    assertTrue(result.contains("# client-id: also-should-not-change"));
  }

  // -------------------------------------------------------------------------
  // transformConfig — provider > molgenis > issuer-uri
  // -------------------------------------------------------------------------

  @Test
  void transformConfig_updatesProviderMolgenisIssuerUri() {
    List<String> lines =
        Arrays.asList(
            "        provider:",
            "          molgenis:",
            "            issuer-uri: https://old-issuer.example.com");
    String result = updater.transformConfig(lines, oidcDetails);
    assertTrue(result.contains("issuer-uri: https://new-issuer.example.com"));
    assertFalse(result.contains("https://old-issuer.example.com"));
  }

  // -------------------------------------------------------------------------
  // transformConfig — registration > molgenis > client-id & client-secret
  // -------------------------------------------------------------------------

  @Test
  void transformConfig_updatesRegistrationMolgenisClientId() {
    List<String> lines =
        Arrays.asList(
            "        registration:", "          molgenis:", "            client-id: old-client-id");
    String result = updater.transformConfig(lines, oidcDetails);
    assertTrue(result.contains("client-id: new-client-id"));
    assertFalse(result.contains("old-client-id"));
  }

  @Test
  void transformConfig_updatesRegistrationMolgenisClientSecret() {
    List<String> lines =
        Arrays.asList(
            "        registration:",
            "          molgenis:",
            "            client-secret: old-secret");
    String result = updater.transformConfig(lines, oidcDetails);
    assertTrue(result.contains("client-secret: new-client-secret"));
    assertFalse(result.contains("old-secret"));
  }

  // -------------------------------------------------------------------------
  // transformConfig — resourceserver > jwt > issuer-uri
  // -------------------------------------------------------------------------

  @Test
  void transformConfig_updatesResourceServerJwtIssuerUri() {
    List<String> lines =
        Arrays.asList(
            "      resourceserver:",
            "        jwt:",
            "          issuer-uri: https://old-device-issuer.example.com");
    String result = updater.transformConfig(lines, oidcDetails);
    assertTrue(result.contains("issuer-uri: https://new-device-issuer.example.com"));
    assertFalse(result.contains("https://old-device-issuer.example.com"));
  }

  // -------------------------------------------------------------------------
  // transformConfig — resourceserver > opaquetoken > client-id
  // -------------------------------------------------------------------------

  @Test
  void transformConfig_updatesResourceServerOpaqueTokenClientId() {
    List<String> lines =
        Arrays.asList(
            "      resourceserver:",
            "        opaquetoken:",
            "          client-id: old-device-client-id");
    String result = updater.transformConfig(lines, oidcDetails);
    assertTrue(result.contains("client-id: new-device-client-id"));
    assertFalse(result.contains("old-device-client-id"));
  }

  // -------------------------------------------------------------------------
  // transformConfig — each field replaced only once (guard flags work)
  // -------------------------------------------------------------------------

  @Test
  void transformConfig_issuerUriUpdatedOnlyOnceForProvider() {
    List<String> lines =
        Arrays.asList(
            "        provider:",
            "          molgenis:",
            "            issuer-uri: https://first-old.example.com",
            // second occurrence — should NOT be replaced
            "            issuer-uri: https://second-old.example.com");
    String result = updater.transformConfig(lines, oidcDetails);
    long updatedCount =
        Arrays.stream(result.split(System.lineSeparator()))
            .filter(l -> l.contains("issuer-uri: https://new-issuer.example.com"))
            .count();
    assertEquals(1, updatedCount, "Provider issuer-uri must be replaced exactly once");
    assertTrue(
        result.contains("https://second-old.example.com"),
        "Second occurrence should remain unchanged");
  }

  @Test
  void transformConfig_clientIdUpdatedOnlyOnceForRegistration() {
    List<String> lines =
        Arrays.asList(
            "        registration:",
            "          molgenis:",
            "            client-id: old-client-id-1",
            "            client-id: old-client-id-2");
    String result = updater.transformConfig(lines, oidcDetails);
    long count =
        Arrays.stream(result.split(System.lineSeparator()))
            .filter(l -> l.contains("client-id: new-client-id"))
            .count();
    assertEquals(1, count);
  }

  // -------------------------------------------------------------------------
  // transformConfig — unrelated lines survive unchanged
  // -------------------------------------------------------------------------

  @Test
  void transformConfig_doesNotModifyUnrelatedLines() {
    List<String> lines = Arrays.asList("server:", "  port: 8080", "logging:", "  level: INFO");
    String result = updater.transformConfig(lines, oidcDetails);
    assertTrue(result.contains("server:"));
    assertTrue(result.contains("  port: 8080"));
    assertTrue(result.contains("logging:"));
    assertTrue(result.contains("  level: INFO"));
  }

  // -------------------------------------------------------------------------
  // Full integration: updateApplicationConfig writes file and backup correctly
  // -------------------------------------------------------------------------

  @Test
  void updateApplicationConfig_writesUpdatedFileAndBackup() throws IOException {
    String nl = System.lineSeparator();
    String configContent =
        "spring:"
            + nl
            + "  security:"
            + nl
            + "    oauth2:"
            + nl
            + "      client:"
            + nl
            + "        provider:"
            + nl
            + "          molgenis:"
            + nl
            + "            issuer-uri: https://old-issuer.example.com"
            + nl
            + "        registration:"
            + nl
            + "          molgenis:"
            + nl
            + "            client-id: old-client-id"
            + nl
            + "            client-secret: old-secret"
            + nl
            + "      resourceserver:"
            + nl
            + "        jwt:"
            + nl
            + "          issuer-uri: https://old-device-issuer.example.com"
            + nl
            + "        opaquetoken:"
            + nl
            + "          client-id: old-device-client-id"
            + nl;

    Path configFile = tempDir.resolve("application.yml");
    Files.writeString(configFile, configContent);

    ApplicationConfigUpdater fileUpdater = new ApplicationConfigUpdater(configFile.toString());
    fileUpdater.updateApplicationConfig(oidcDetails);

    // Backup must exist and preserve original content exactly
    Path backupFile = tempDir.resolve("application.yml.bak");
    assertTrue(backupFile.toFile().exists(), "Backup file should be created");
    assertEquals(configContent, Files.readString(backupFile));

    // Updated file must contain all new values
    String updated = Files.readString(configFile);
    assertTrue(updated.contains("issuer-uri: https://new-issuer.example.com"));
    assertTrue(updated.contains("issuer-uri: https://new-device-issuer.example.com"));
    assertTrue(updated.contains("client-id: new-client-id"));
    assertTrue(updated.contains("client-secret: new-client-secret"));
    assertTrue(updated.contains("client-id: new-device-client-id"));
  }

  @Test
  void updateApplicationConfig_throwsRuntimeException_whenFileDoesNotExist() {
    ApplicationConfigUpdater badUpdater =
        new ApplicationConfigUpdater("/nonexistent/path/application.yml");
    assertThrows(RuntimeException.class, () -> badUpdater.updateApplicationConfig(oidcDetails));
  }
}
