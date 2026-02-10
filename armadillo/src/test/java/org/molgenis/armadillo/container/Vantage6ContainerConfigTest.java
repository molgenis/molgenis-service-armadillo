package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;

class Vantage6ContainerConfigTest {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void testCreateDefault() {
    var config = Vantage6ContainerConfig.createDefault();

    assertEquals("default", config.getName());
    assertEquals("molgenis/armadillo-v6-bridge", config.getImage());
    assertEquals("localhost", config.getHost());
    assertEquals(8081, config.getPort());
    assertEquals("v6", config.getType());
    assertEquals(Set.of(), config.getAllowedAlgorithms());
    assertEquals(Set.of(), config.getAllowedAlgorithmStores());
    assertEquals(Set.of(), config.getAuthorizedProjects());
  }

  @Test
  void testCreateWithV6SpecificFields() {
    var config =
        Vantage6ContainerConfig.builder()
            .name("v6-node")
            .serverUrl("https://v6.example.org")
            .apiKey("test-api-key")
            .collaborationId(42)
            .encryptionKey("/path/to/key.pem")
            .allowedAlgorithms(Set.of("harbor.v6.ai/algo/.*"))
            .allowedAlgorithmStores(Set.of("https://store.v6.ai"))
            .authorizedProjects(Set.of("shared-projectA", "shared-projectB"))
            .build();

    assertEquals("v6-node", config.getName());
    assertEquals("https://v6.example.org", config.getServerUrl());
    assertEquals("test-api-key", config.getApiKey());
    assertEquals(42, config.getCollaborationId());
    assertEquals("/path/to/key.pem", config.getEncryptionKey());
    assertEquals(Set.of("harbor.v6.ai/algo/.*"), config.getAllowedAlgorithms());
    assertEquals(Set.of("https://store.v6.ai"), config.getAllowedAlgorithmStores());
    assertEquals(Set.of("shared-projectA", "shared-projectB"), config.getAuthorizedProjects());
  }

  @Test
  void testJsonSerializationRoundTrip() throws Exception {
    var original =
        Vantage6ContainerConfig.builder()
            .name("test-v6")
            .serverUrl("https://v6.example.org")
            .apiKey("key-123")
            .collaborationId(1)
            .authorizedProjects(Set.of("shared-test"))
            .build();

    String json = objectMapper.writeValueAsString(original);

    // Verify type discriminator is present
    assertTrue(json.contains("\"type\":\"v6\""));

    // Deserialize through the polymorphic interface
    ContainerConfig deserialized = objectMapper.readValue(json, ContainerConfig.class);

    assertInstanceOf(Vantage6ContainerConfig.class, deserialized);
    var v6Config = (Vantage6ContainerConfig) deserialized;

    assertEquals("test-v6", v6Config.getName());
    assertEquals("https://v6.example.org", v6Config.getServerUrl());
    assertEquals("key-123", v6Config.getApiKey());
    assertEquals(1, v6Config.getCollaborationId());
    assertEquals(Set.of("shared-test"), v6Config.getAuthorizedProjects());
    assertEquals("v6", v6Config.getType());
  }

  @Test
  void testToBuilder() {
    var original =
        Vantage6ContainerConfig.builder()
            .name("v6-node")
            .serverUrl("https://old.example.org")
            .apiKey("old-key")
            .build();

    var updated =
        original.toBuilder().serverUrl("https://new.example.org").apiKey("new-key").build();

    assertEquals("v6-node", updated.getName());
    assertEquals("https://new.example.org", updated.getServerUrl());
    assertEquals("new-key", updated.getApiKey());
  }

  @Test
  void testPolymorphicDeserialization() throws Exception {
    String json =
        """
        {
          "type": "v6",
          "name": "my-v6-node",
          "serverUrl": "https://v6.example.org",
          "apiKey": "abc123",
          "collaborationId": 5,
          "authorizedProjects": ["shared-proj1"]
        }
        """;

    ContainerConfig config = objectMapper.readValue(json, ContainerConfig.class);

    assertInstanceOf(Vantage6ContainerConfig.class, config);
    assertEquals("my-v6-node", config.getName());
    assertEquals("v6", config.getType());

    var v6 = (Vantage6ContainerConfig) config;
    assertEquals("https://v6.example.org", v6.getServerUrl());
    assertEquals("abc123", v6.getApiKey());
    assertEquals(5, v6.getCollaborationId());
    assertEquals(Set.of("shared-proj1"), v6.getAuthorizedProjects());
  }
}
