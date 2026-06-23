package org.molgenis.armadillo.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.molgenis.armadillo.metadata.OidcDetails;

public class ApplicationConfigUpdater {
  String armadilloConfigFile;
  String backupExt = ".bak";

  // Simple mutable state carrier — private inner class or a record (Java 16+)
  private static class ConfigParseState {
    boolean providerFound;
    boolean providerMolgenisFound;
    boolean registrationFound;
    boolean registrationMolgenisFound;
    boolean resourceServerFound;
    boolean resourceServerJwtFound;
    boolean resourceServerOpaqueFound;
    boolean clientIdUpdated;
    boolean clientSecretUpdated;
    boolean issuerUriUpdated;
    boolean deviceIssuerUriUpdated;
    boolean deviceClientIdUpdated;
  }

  public ApplicationConfigUpdater(String armadilloConfigFile) {
    this.armadilloConfigFile = armadilloConfigFile;
  }

  public void updateApplicationConfig(OidcDetails oidcDetails) {
    try (BufferedReader br = new BufferedReader(new FileReader(armadilloConfigFile))) {
      List<String> lines = br.lines().toList();
      String existingConfig = String.join(System.lineSeparator(), lines) + System.lineSeparator();
      String newConfig = transformConfig(lines, oidcDetails);

      FileUtils.writeStringToFile(
          new File(armadilloConfigFile + backupExt),
          existingConfig,
          Charset.defaultCharset(),
          false);
      FileUtils.writeStringToFile(
          new File(armadilloConfigFile), newConfig, Charset.defaultCharset(), false);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  String transformConfig(List<String> lines, OidcDetails oidcDetails) {
    ConfigParseState state = new ConfigParseState();
    StringBuilder newConfig = new StringBuilder();

    for (String line : lines) {
      newConfig.append(transformLine(line, state, oidcDetails));
      newConfig.append(System.lineSeparator());
    }
    return newConfig.toString();
  }

  String transformLine(String line, ConfigParseState state, OidcDetails oidcDetails) {
    if (line.strip().startsWith("#")) return line;

    updateState(line, state);
    return applyTransformation(line, state, oidcDetails);
  }

  void updateState(String line, ConfigParseState state) {
    if (line.contains("provider:")) state.providerFound = true;
    if (line.contains("registration:")) state.registrationFound = true;
    if (line.contains("resourceserver:")) state.resourceServerFound = true;
    if (line.contains("jwt:") && state.resourceServerFound) state.resourceServerJwtFound = true;
    if (line.contains("opaquetoken") && state.resourceServerFound)
      state.resourceServerOpaqueFound = true;
    if (line.contains("molgenis:")) {
      if (state.providerFound) state.providerMolgenisFound = true;
      if (state.registrationFound) state.registrationMolgenisFound = true;
    }
  }

  String applyTransformation(String line, ConfigParseState state, OidcDetails oidcDetails) {
    if (line.contains("issuer-uri:")) {
      if (state.providerMolgenisFound && !state.issuerUriUpdated) {
        state.issuerUriUpdated = true;
        return replaceValue(line, oidcDetails.getIssuerUri());
      }
      if (state.resourceServerJwtFound && !state.deviceIssuerUriUpdated) {
        state.deviceIssuerUriUpdated = true;
        return replaceValue(line, oidcDetails.getDeviceIssuerUri());
      }
    }
    if (line.contains("client-id:")) {
      if (state.registrationMolgenisFound && !state.clientIdUpdated) {
        state.clientIdUpdated = true;
        return replaceValue(line, oidcDetails.getClientId());
      }
      if (state.resourceServerOpaqueFound && !state.deviceClientIdUpdated) {
        state.deviceClientIdUpdated = true;
        return replaceValue(line, oidcDetails.getDeviceClientId());
      }
    }
    if (line.contains("client-secret:")
        && state.registrationMolgenisFound
        && !state.clientSecretUpdated) {
      state.clientSecretUpdated = true;
      return replaceValue(line, oidcDetails.getClientSecret());
    }
    return line;
  }

  String replaceValue(String line, String newValue) {
    return line.split(":")[0] + ": " + newValue;
  }
}
