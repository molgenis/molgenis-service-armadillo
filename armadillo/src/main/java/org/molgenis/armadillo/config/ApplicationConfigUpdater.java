package org.molgenis.armadillo.config;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import org.molgenis.armadillo.metadata.OidcDetails;
import org.yaml.snakeyaml.Yaml;

public class ApplicationConfigUpdater {

  private static final String BACKUP_EXT = ".bak";

  private final String armadilloConfigFile;

  private LinkedHashMap<
          String,
          LinkedHashMap<
              String,
              LinkedHashMap<
                  String,
                  LinkedHashMap<
                      String,
                      LinkedHashMap<
                          String, LinkedHashMap<String, LinkedHashMap<String, Object>>>>>>>
      config;

  public ApplicationConfigUpdater(String armadilloConfigFile) {
    this.armadilloConfigFile = armadilloConfigFile;
  }

  void updateConfig(OidcDetails oidcDetails) {
    String issuerUri = "issuer-uri";
    String clientId = "client-id";
    String molgenis = "molgenis";

    LinkedHashMap<
            String,
            LinkedHashMap<
                String,
                LinkedHashMap<
                    String,
                    LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, Object>>>>>>
        springConfig = config.get("spring");
    LinkedHashMap<
            String,
            LinkedHashMap<
                String,
                LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, Object>>>>>
        securityConfig = springConfig.get("security");
    LinkedHashMap<String, LinkedHashMap<String, LinkedHashMap<String, Object>>> oauthConfig =
        (LinkedHashMap) securityConfig.get("oauth2");
    LinkedHashMap<String, LinkedHashMap<String, Object>> clientConfig = oauthConfig.get("client");

    LinkedHashMap<String, LinkedHashMap<String, Object>> providerConfig =
        (LinkedHashMap) clientConfig.get("provider");
    LinkedHashMap<String, Object> providerMolgenisConfig = providerConfig.get(molgenis);

    LinkedHashMap<String, LinkedHashMap<String, Object>> registrationConfig =
        (LinkedHashMap) clientConfig.get("registration");
    LinkedHashMap<String, Object> registrationMolgenisConfig = registrationConfig.get(molgenis);

    LinkedHashMap<String, LinkedHashMap<String, Object>> resourceserverConfig =
        oauthConfig.get("resourceserver");
    LinkedHashMap<String, Object> jwtConfig = resourceserverConfig.get("jwt");
    LinkedHashMap<String, Object> opaquetokenConfig = resourceserverConfig.get("opaquetoken");

    providerMolgenisConfig.put(issuerUri, oidcDetails.getIssuerUri());
    registrationMolgenisConfig.put(clientId, oidcDetails.getClientId());
    registrationMolgenisConfig.put("client-secret", oidcDetails.getClientSecret());
    opaquetokenConfig.put(clientId, oidcDetails.getDeviceClientId());
    jwtConfig.put(issuerUri, oidcDetails.getDeviceIssuerUri());
  }

  void writeConfigFile(String path) throws IOException {
    Yaml yaml = new Yaml();
    FileWriter writer = new FileWriter(path);
    yaml.dump(config, writer);
  }

  public void updateApplicationConfig(OidcDetails oidcDetails) {
    try {
      readConfigFile();
      writeConfigFile(armadilloConfigFile + BACKUP_EXT);
      updateConfig(oidcDetails);
      writeConfigFile(armadilloConfigFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void readConfigFile() {
    Yaml yaml = new Yaml();
    try (InputStream in = Files.newInputStream(Paths.get(armadilloConfigFile))) {
      config = yaml.load(in);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public LinkedHashMap<
          String,
          LinkedHashMap<
              String,
              LinkedHashMap<
                  String,
                  LinkedHashMap<
                      String,
                      LinkedHashMap<
                          String, LinkedHashMap<String, LinkedHashMap<String, Object>>>>>>>
      getConfig() {
    return new LinkedHashMap<>(config);
  }
}
