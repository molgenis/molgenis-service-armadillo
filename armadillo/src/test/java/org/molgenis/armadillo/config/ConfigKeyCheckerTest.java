package org.molgenis.armadillo.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

class ConfigKeyCheckerTest {

  static ConfigKeyChecker checkerFor(Resource... yamlFiles) throws IOException {
    return new ConfigKeyChecker(environmentWith(yamlFiles));
  }

  private static ConfigurableEnvironment environmentWith(Resource... yamlFiles) throws IOException {
    ConfigurableEnvironment environment = new StandardEnvironment();
    for (Resource file : yamlFiles) {
      new YamlPropertySourceLoader()
          .load(file.getDescription(), file)
          .forEach(environment.getPropertySources()::addLast);
    }
    return environment;
  }

  private static Resource yamlFile(String name, String content) {
    return new ByteArrayResource(content.getBytes(UTF_8), name);
  }

  private static List<String> findConfigKeyProblems(String yaml) throws IOException {
    return checkerFor(yamlFile("test-application.yml", yaml)).findConfigKeyProblems();
  }

  private static void assertSingleProblemAbout(String key, List<String> problems) {
    assertEquals(1, problems.size(), problems.toString());
    assertTrue(problems.get(0).contains(key), problems.get(0));
  }

  private static void assertAnyProblemContains(String text, List<String> problems) {
    assertTrue(problems.stream().anyMatch(p -> p.contains(text)), problems.toString());
  }

  @Test
  void reportsNothingForCorrectConfig() throws IOException {
    String yaml =
        """
        spring:
          servlet:
            multipart:
              max-file-size: 1MB
        storage:
          root-dir: data
        """;
    assertEquals(List.of(), findConfigKeyProblems(yaml));
  }

  @Test
  void reportsKeyUnderWrongParent() throws IOException {
    String yaml =
        """
        spring:
          security:
            servlet:
              multipart:
                max-file-size: 1MB
        """;
    assertSingleProblemAbout(
        "spring.security.servlet.multipart.max-file-size", findConfigKeyProblems(yaml));
  }

  @Test
  void reportsMisspeltKey() throws IOException {
    String yaml = """
        storage:
          root-dri: data
        """;
    assertSingleProblemAbout("storage.root-dri", findConfigKeyProblems(yaml));
  }

  @Test
  void reportsFileAndLineOfKey() throws IOException {
    String yaml =
        """
        storage:
          root-dir: data
        armadillo:
          docker-managment-enabled: false
        """;
    List<String> problems = findConfigKeyProblems(yaml);
    assertSingleProblemAbout("armadillo.docker-managment-enabled", problems);
    assertTrue(problems.get(0).contains("test-application.yml"), problems.get(0));
    assertTrue(problems.get(0).contains("4:"), problems.get(0));
  }

  @Test
  void ignoresPropertySourcesThatAreNotFiles() {
    ConfigurableEnvironment environment = new StandardEnvironment();
    environment
        .getPropertySources()
        .addLast(new MapPropertySource("other", Map.of("bogus.key", "x")));
    assertEquals(List.of(), new ConfigKeyChecker(environment).findConfigKeyProblems());
  }

  @Test
  void acceptsCompleteOidcConfig() throws IOException {
    String yaml =
        """
        spring:
          security:
            oauth2:
              client:
                provider:
                  molgenis:
                    issuer-uri: http://auth.example.org
                registration:
                  molgenis:
                    client-id: id
                    client-secret: secret
              resourceserver:
                jwt:
                  issuer-uri: http://auth.example.org
                opaquetoken:
                  client-id: id
        """;
    assertEquals(List.of(), findConfigKeyProblems(yaml));
  }

  @Test
  void reportsMisspeltKeyInsideOidcRegistration() throws IOException {
    String yaml =
        """
        spring:
          security:
            oauth2:
              client:
                registration:
                  molgenis:
                    client-id: id
                    clientt-secret: secret
        """;
    assertSingleProblemAbout(
        "spring.security.oauth2.client.registration.molgenis.clientt-secret",
        findConfigKeyProblems(yaml));
  }

  @Test
  void keepsReportingOtherProblemsWhenOneCheckFails() throws IOException {
    String yaml =
        """
        storage:
          root-dri: data
        spring:
          security:
            oauth2:
              client:
                registration: not-a-map
        """;
    List<String> problems = findConfigKeyProblems(yaml);
    assertEquals(2, problems.size(), problems.toString());
    assertAnyProblemContains("storage.root-dri", problems);
    assertAnyProblemContains("Could not check", problems);
  }

  @Test
  void reportsMisspeltProfileFields() throws IOException {
    String yaml =
        """
        armadillo:
          profiles:
            - name: default
              imgae: datashield/rock-base:latest
              prot: 6311
        """;
    List<String> problems = findConfigKeyProblems(yaml);
    assertEquals(2, problems.size(), problems.toString());
    assertAnyProblemContains("armadillo.profiles[0].imgae", problems);
    assertAnyProblemContains("armadillo.profiles[0].prot", problems);
  }

  @Test
  void acceptsAnyProfileOptionAndAlternativeSpellings() throws IOException {
    String yaml =
        """
        armadillo:
          profiles:
            - name: default
              autoUpdate: true
              packageWhitelist:
                - dsBase
              options:
                anything-at-all: x
                datashield:
                  seed: 342325352
        """;
    assertEquals(List.of(), findConfigKeyProblems(yaml));
  }

  @Test
  void acceptsProfilesListedInTwoFiles() throws IOException {
    ConfigKeyChecker checker =
        checkerFor(
            yamlFile("first.yml", "armadillo.profiles:\n  - name: one\n"),
            yamlFile("second.yml", "armadillo.profiles:\n  - name: two\n  - name: three\n"));
    assertEquals(List.of(), checker.findConfigKeyProblems());
  }

  @Test
  void acceptsPlaceholderValueDefinedElsewhere() throws IOException {
    ConfigurableEnvironment environment =
        environmentWith(
            yamlFile(
                "test-application.yml",
                "armadillo.profiles:\n  - name: one\n    port: ${PROFILE_PORT}\n"));
    environment
        .getPropertySources()
        .addLast(new MapPropertySource("other", Map.of("PROFILE_PORT", "6311")));
    assertEquals(List.of(), new ConfigKeyChecker(environment).findConfigKeyProblems());
  }

  @Test
  void reportsKeyFoundByTwoChecksOnlyOnce() throws IOException {
    String yaml =
        """
        spring:
          security:
            oauth2:
              client:
                registraton:
                  molgenis:
                    client-id: id
        """;
    assertSingleProblemAbout("registraton.molgenis.client-id", findConfigKeyProblems(yaml));
  }
}
