package org.molgenis.armadillo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.yaml.snakeyaml.Yaml;

/**
 * NOTE: This test lives in the same package as ApplicationConfigUpdater on purpose, so it can reach
 * the package-private fields/methods (config, readConfigFile, updateConfig, writeConfigFile)
 * directly without reflection.
 *
 * <p>OidcDetails is an AutoValue class with no public constructor; instances are built via the
 * static OidcDetails.create(issuerUri, clientId, clientSecret, deviceIssuerUri, deviceClientId)
 * factory (note deviceIssuerUri comes before deviceClientId).
 */
class ApplicationConfigUpdaterTest {

  private static final String YAML_CONTENT =
      """
spring:
  session: {timeout: 1m}
  mvc:
    async: {request-timeout: 36000000}
  servlet:
    multipart: {max-file-size: 13000MB, max-request-size: 13000MB}
  security:
    user: {password: admin}
    oauth2:
      client:
        provider:
          molgenis: {issuer-uri: 'old-issuer'}
        registration:
          molgenis:
            client-id: old-client-id
            client-secret: old-secret
            authorization-grant-type: [authorization_code, refresh_token]
      resourceserver:
        jwt: {issuer-uri: 'old-device-issuer'}
        opaquetoken: {client-id: old-device-client-id}""";

  @TempDir Path tempDir;

  private Path configFile;
  private ApplicationConfigUpdater updater;

  @BeforeEach
  void setUp() throws IOException {
    configFile = tempDir.resolve("application.yml");
    Files.writeString(configFile, YAML_CONTENT);
    updater = new ApplicationConfigUpdater(configFile.toString());
  }

  private OidcDetails buildOidcDetails(
      String issuerUri,
      String clientId,
      String clientSecret,
      String deviceIssuerUri,
      String deviceClientId) {
    return OidcDetails.create(issuerUri, clientId, clientSecret, deviceIssuerUri, deviceClientId);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getNested(Object map, String... keys) {
    Object current = map;
    for (String key : keys) {
      current = ((Map<String, Object>) current).get(key);
    }
    return (Map<String, Object>) current;
  }

  // ---------- readConfigFile ----------

  @Test
  void readConfigFile_loadsYamlIntoConfig() {
    updater.readConfigFile();

    assertNotNull(updater.config);
    assertTrue(updater.config.containsKey("spring"));
    Map<String, Object> provider =
        getNested(updater.config, "spring", "security", "oauth2", "client", "provider", "molgenis");
    assertEquals("old-issuer", provider.get("issuer-uri"));
  }

  @Test
  void readConfigFile_missingFile_throwsRuntimeException() {
    ApplicationConfigUpdater badUpdater =
        new ApplicationConfigUpdater(tempDir.resolve("does-not-exist.yml").toString());

    assertThrows(RuntimeException.class, badUpdater::readConfigFile);
  }

  // ---------- updateConfig ----------

  @Test
  void updateConfig_updatesAllExpectedFieldsInPlace() {
    updater.readConfigFile();

    OidcDetails oidcDetails =
        buildOidcDetails(
            "https://new-issuer",
            "new-client-id",
            "new-secret",
            "https://new-device-issuer",
            "new-device-client-id");

    updater.updateConfig(oidcDetails);

    Map<String, Object> provider =
        getNested(updater.config, "spring", "security", "oauth2", "client", "provider", "molgenis");
    assertEquals("https://new-issuer", provider.get("issuer-uri"));

    Map<String, Object> registration =
        getNested(
            updater.config, "spring", "security", "oauth2", "client", "registration", "molgenis");
    assertEquals("new-client-id", registration.get("client-id"));
    assertEquals("new-secret", registration.get("client-secret"));

    Map<String, Object> opaquetoken =
        getNested(updater.config, "spring", "security", "oauth2", "resourceserver", "opaquetoken");
    assertEquals("new-device-client-id", opaquetoken.get("client-id"));

    Map<String, Object> jwt =
        getNested(updater.config, "spring", "security", "oauth2", "resourceserver", "jwt");
    assertEquals("https://new-device-issuer", jwt.get("issuer-uri"));
  }

  @Test
  void updateConfig_doesNotTouchUnrelatedKeys() {
    updater.readConfigFile();
    Map<String, Object> springBefore = getNested(updater.config, "spring");
    assertTrue(springBefore.containsKey("security"));

    updater.updateConfig(buildOidcDetails("i", "c", "s", "di", "dc"));

    // top level "spring" -> "security" structure should still be present/unchanged in shape
    Map<String, Object> springAfter = getNested(updater.config, "spring");
    assertTrue(springAfter.containsKey("security"));
  }

  // ---------- writeConfigFile ----------

  @Test
  void writeConfigFile_writesReadableYaml() throws IOException {
    updater.readConfigFile();
    Path outputFile = tempDir.resolve("output.yml");

    updater.writeConfigFile(outputFile.toString());

    assertTrue(Files.exists(outputFile));
    Yaml yaml = new Yaml();
    Map<String, Object> written;
    try (InputStream in = Files.newInputStream(outputFile)) {
      written = yaml.load(in);
    }
    assertNotNull(written);
    assertTrue(written.containsKey("spring"));
  }

  // ---------- updateApplicationConfig (full round trip) ----------

  @Test
  void updateApplicationConfig_writesBackupWithOriginalValues() throws IOException {
    OidcDetails oidcDetails =
        buildOidcDetails(
            "https://new-issuer",
            "new-client-id",
            "new-secret",
            "https://new-device-issuer",
            "new-device-client-id");

    updater.updateApplicationConfig(oidcDetails);

    Path backupFile = tempDir.resolve("application.yml.bak");
    assertTrue(Files.exists(backupFile));

    Yaml yaml = new Yaml();
    Map<String, Object> backupConfig;
    try (InputStream in = Files.newInputStream(backupFile)) {
      backupConfig = yaml.load(in);
    }
    Map<String, Object> backupProvider =
        getNested(backupConfig, "spring", "security", "oauth2", "client", "provider", "molgenis");
    assertEquals("old-issuer", backupProvider.get("issuer-uri"));
  }

  @Test
  void updateApplicationConfig_overwritesOriginalFileWithUpdatedValues() throws IOException {
    OidcDetails oidcDetails =
        buildOidcDetails(
            "https://new-issuer",
            "new-client-id",
            "new-secret",
            "https://new-device-issuer",
            "new-device-client-id");

    updater.updateApplicationConfig(oidcDetails);

    Yaml yaml = new Yaml();
    Map<String, Object> updatedConfig;
    try (InputStream in = Files.newInputStream(configFile)) {
      updatedConfig = yaml.load(in);
    }

    Map<String, Object> provider =
        getNested(updatedConfig, "spring", "security", "oauth2", "client", "provider", "molgenis");
    assertEquals("https://new-issuer", provider.get("issuer-uri"));

    Map<String, Object> registration =
        getNested(
            updatedConfig, "spring", "security", "oauth2", "client", "registration", "molgenis");
    assertEquals("new-client-id", registration.get("client-id"));
    assertEquals("new-secret", registration.get("client-secret"));

    Map<String, Object> opaquetoken =
        getNested(updatedConfig, "spring", "security", "oauth2", "resourceserver", "opaquetoken");
    assertEquals("new-device-client-id", opaquetoken.get("client-id"));

    Map<String, Object> jwt =
        getNested(updatedConfig, "spring", "security", "oauth2", "resourceserver", "jwt");
    assertEquals("https://new-device-issuer", jwt.get("issuer-uri"));
  }

  @Test
  void updateApplicationConfig_missingSourceFile_throwsRuntimeException() {
    ApplicationConfigUpdater badUpdater =
        new ApplicationConfigUpdater(tempDir.resolve("does-not-exist.yml").toString());

    OidcDetails oidcDetails = buildOidcDetails("i", "c", "s", "di", "dc");

    assertThrows(RuntimeException.class, () -> badUpdater.updateApplicationConfig(oidcDetails));
  }

  @Test
  void updateConfig_withEmptyOidcDetails_writesEmptyStrings() {
    updater.readConfigFile();

    // OidcDetails.create() with no args produces the "not configured yet" default: all fields ""
    updater.updateConfig(OidcDetails.create());

    Map<String, Object> provider =
        getNested(updater.config, "spring", "security", "oauth2", "client", "provider", "molgenis");
    assertEquals("", provider.get("issuer-uri"));

    Map<String, Object> registration =
        getNested(
            updater.config, "spring", "security", "oauth2", "client", "registration", "molgenis");
    assertEquals("", registration.get("client-id"));
    assertEquals("", registration.get("client-secret"));
  }
}
