package org.molgenis.armadillo.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.molgenis.armadillo.metadata.OidcDetails;

public class ApplicationConfigFile implements ConfigFile {

  private static final String BACKUP_EXT = ".bak";

  private static final YAMLFactory yamlFactory = new YAMLFactory();
  private static final ObjectMapper objectMapper = new ObjectMapper(yamlFactory);

  private final String armadilloConfigFile;
  private JsonNode config;

  public ApplicationConfigFile(String armadilloConfigFile) {
    this.armadilloConfigFile = armadilloConfigFile;
  }

  public void write(OidcDetails oidcDetails) {
    String issuerUri = "issuer-uri";
    String clientId = "client-id";

    JsonNode oauthConfig = config.at("/spring/security/oauth2");
    JsonNode providerMolgenisConfig = oauthConfig.at("/client/provider/molgenis");
    JsonNode registrationMolgenisConfig = oauthConfig.at("/client/registration/molgenis");
    JsonNode jwtConfig = oauthConfig.at("/resourceserver/jwt");
    JsonNode opaqueTokenConfig = oauthConfig.at("/resourceserver/opaquetoken");

    if (!(providerMolgenisConfig instanceof ObjectNode)) {
      throw new ConfigUpdateException(
          "Unable to update provider molgenis config in oauth2 configuration");
    }
    ((ObjectNode) providerMolgenisConfig).put(issuerUri, oidcDetails.getIssuerUri());

    if (!(registrationMolgenisConfig instanceof ObjectNode)) {
      throw new ConfigUpdateException(
          "Unable to update registration molgenis config in oauth2 configuration");
    }
    ((ObjectNode) registrationMolgenisConfig)
        .put(clientId, oidcDetails.getClientId())
        .put("client-secret", oidcDetails.getClientSecret());

    if (!(opaqueTokenConfig instanceof ObjectNode)) {
      throw new ConfigUpdateException(
          "Unable to update opaquetoken config in oauth2 configuration");
    }
    ((ObjectNode) opaqueTokenConfig).put(clientId, oidcDetails.getDeviceClientId());

    if (!(jwtConfig instanceof ObjectNode)) {
      throw new ConfigUpdateException("Unable to update jwt config in oauth2 configuration");
    }
    ((ObjectNode) jwtConfig).put(issuerUri, oidcDetails.getDeviceIssuerUri());
  }

  void write(String path) throws IOException {
    FileWriter writer = new FileWriter(path);

    try (YAMLGenerator generator = yamlFactory.createGenerator(writer)) {
      generator.writeObject(config);
    }
  }

  @Override
  public void update(OidcDetails oidcDetails) {
    try {
      read();
      write(armadilloConfigFile + BACKUP_EXT);
      write(oidcDetails);
      write(armadilloConfigFile);
    } catch (Exception e) {
      throw new ConfigUpdateException(e);
    }
  }

  void read() {
    try (InputStream in = Files.newInputStream(Paths.get(armadilloConfigFile))) {
      config = objectMapper.readTree(in);
    } catch (IOException e) {
      throw new ConfigUpdateException(e);
    }
  }

  @Override
  public Map<String, Object> getConfig() {
    if (config == null) {
      read();
    }

    return objectMapper.convertValue(config, new TypeReference<>() {});
  }

  private static final class ConfigUpdateException extends RuntimeException {
    public ConfigUpdateException(String message) {
      super(message);
    }

    public ConfigUpdateException(Throwable cause) {
      super(cause);
    }
  }
}
