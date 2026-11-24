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

  static ConfigKeyChecker checkerFor(Resource yaml) throws IOException {
    ConfigurableEnvironment environment = new StandardEnvironment();
    new YamlPropertySourceLoader()
        .load("test-application.yml", yaml)
        .forEach(environment.getPropertySources()::addLast);
    return new ConfigKeyChecker(environment);
  }

  private static List<String> findProblems(String yaml) throws IOException {
    return checkerFor(new ByteArrayResource(yaml.getBytes(UTF_8), "test-application.yml"))
        .findProblems();
  }

  private static void assertSingleProblemAbout(String key, List<String> problems) {
    assertEquals(1, problems.size(), problems.toString());
    assertTrue(problems.get(0).contains(key), problems.get(0));
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
    assertEquals(List.of(), findProblems(yaml));
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
    assertSingleProblemAbout("spring.security.servlet.multipart.max-file-size", findProblems(yaml));
  }

  @Test
  void reportsMisspeltKey() throws IOException {
    String yaml = """
        storage:
          root-dri: data
        """;
    assertSingleProblemAbout("storage.root-dri", findProblems(yaml));
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
    List<String> problems = findProblems(yaml);
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
    assertEquals(List.of(), new ConfigKeyChecker(environment).findProblems());
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
    assertEquals(List.of(), findProblems(yaml));
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
        "spring.security.oauth2.client.registration.molgenis.clientt-secret", findProblems(yaml));
  }

  @Test
  void acceptsCompleteProfile() throws IOException {
    String yaml =
        """
        armadillo:
          profiles:
            - name: default
              image: datashield/rock-base:latest
              port: 6311
              package-whitelist:
                - dsBase
              function-blacklist: [ ]
              options:
                datashield:
                  seed: 342325352
        """;
    assertEquals(List.of(), findProblems(yaml));
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
    List<String> problems = findProblems(yaml);
    assertEquals(2, problems.size(), problems.toString());
    assertTrue(
        problems.stream().anyMatch(p -> p.contains("storage.root-dri")), problems.toString());
    assertTrue(
        problems.stream().anyMatch(p -> p.startsWith("Could not check")), problems.toString());
  }
}
