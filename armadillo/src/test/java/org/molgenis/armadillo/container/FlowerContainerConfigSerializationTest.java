package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.ContainersMetadata;

class FlowerContainerConfigSerializationTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void supernodeSerializesWithCorrectType() throws Exception {
    FlowerSupernodeContainerConfig config =
        FlowerSupernodeContainerConfig.builder()
            .name("flower-supernode")
            .image("flwr/supernode:1.26.1")
            .dockerArgs(
                List.of(
                    "--insecure",
                    "--superlink",
                    "host.docker.internal:9092",
                    "--isolation",
                    "process"))
            .build();

    ContainersMetadata metadata = ContainersMetadata.create();
    metadata.getContainers().put("flower-supernode", config);

    String json = OBJECT_MAPPER.writeValueAsString(metadata);
    JsonNode containerNode =
        OBJECT_MAPPER.readTree(json).path("containers").path("flower-supernode");

    assertEquals("flower-supernode", containerNode.path("type").asText());
    assertEquals("flwr/supernode:1.26.1", containerNode.path("image").asText());
    assertTrue(containerNode.path("port").isNull());

    List<String> args =
        StreamSupport.stream(containerNode.path("dockerArgs").spliterator(), false)
            .map(JsonNode::asText)
            .toList();
    assertEquals(
        List.of("--insecure", "--superlink", "host.docker.internal:9092", "--isolation", "process"),
        args);
  }

  @Test
  void superexecSerializesWithCorrectType() throws Exception {
    FlowerSuperexecContainerConfig config =
        FlowerSuperexecContainerConfig.builder()
            .name("flower-project-1")
            .image("timmyjc/superexec-test:0.0.1")
            .dockerArgs(
                List.of(
                    "--plugin-type",
                    "clientapp",
                    "--appio-api-address",
                    "flower-supernode:9094",
                    "--insecure"))
            .build();

    ContainersMetadata metadata = ContainersMetadata.create();
    metadata.getContainers().put("flower-project-1", config);

    String json = OBJECT_MAPPER.writeValueAsString(metadata);
    JsonNode containerNode =
        OBJECT_MAPPER.readTree(json).path("containers").path("flower-project-1");

    assertEquals("flower-superexec", containerNode.path("type").asText());
    assertEquals("timmyjc/superexec-test:0.0.1", containerNode.path("image").asText());
    assertTrue(containerNode.path("port").isNull());
  }

  @Test
  void supernodeDeserializesFromJson() throws Exception {
    String json =
        """
        {
          "containers": {
            "flower-supernode": {
              "type": "flower-supernode",
              "name": "flower-supernode",
              "image": "flwr/supernode:1.26.1",
              "dockerArgs": ["--insecure", "--superlink", "host.docker.internal:9092"]
            }
          }
        }
        """;

    ContainersMetadata metadata = OBJECT_MAPPER.readValue(json, ContainersMetadata.class);
    ContainerConfig config = metadata.getContainers().get("flower-supernode");

    assertInstanceOf(FlowerSupernodeContainerConfig.class, config);
    assertEquals("flower-supernode", config.getName());
    assertEquals("flwr/supernode:1.26.1", config.getImage());
    assertNull(config.getPort());
    assertEquals(
        List.of("--insecure", "--superlink", "host.docker.internal:9092"), config.getDockerArgs());
  }

  @Test
  void superexecDeserializesFromJson() throws Exception {
    String json =
        """
        {
          "containers": {
            "flower-project-1": {
              "type": "flower-superexec",
              "name": "flower-project-1",
              "image": "timmyjc/superexec-test:0.0.1",
              "dockerArgs": ["--plugin-type", "clientapp", "--insecure"]
            }
          }
        }
        """;

    ContainersMetadata metadata = OBJECT_MAPPER.readValue(json, ContainersMetadata.class);
    ContainerConfig config = metadata.getContainers().get("flower-project-1");

    assertInstanceOf(FlowerSuperexecContainerConfig.class, config);
    assertEquals("flower-project-1", config.getName());
    assertEquals("timmyjc/superexec-test:0.0.1", config.getImage());
    assertNull(config.getPort());
    assertEquals(List.of("--plugin-type", "clientapp", "--insecure"), config.getDockerArgs());
  }

  @Test
  void supernodeRoundTrips() throws Exception {
    FlowerSupernodeContainerConfig original =
        FlowerSupernodeContainerConfig.builder()
            .name("flower-supernode")
            .image("flwr/supernode:1.26.1")
            .dockerArgs(List.of("--insecure"))
            .versionId("1.26.1")
            .creationDate("2025-01-01T00:00:00Z")
            .lastImageId("sha256:abc123")
            .imageSize(500_000_000L)
            .installDate("2025-06-01T12:00:00Z")
            .build();

    String json = OBJECT_MAPPER.writeValueAsString(original);
    FlowerSupernodeContainerConfig deserialized =
        OBJECT_MAPPER.readValue(json, FlowerSupernodeContainerConfig.class);

    assertEquals(original, deserialized);
  }

  @Test
  void superexecRoundTrips() throws Exception {
    FlowerSuperexecContainerConfig original =
        FlowerSuperexecContainerConfig.builder()
            .name("flower-project-1")
            .image("timmyjc/superexec-test:0.0.1")
            .dockerArgs(List.of("--insecure"))
            .versionId("0.0.1")
            .creationDate("2025-01-01T00:00:00Z")
            .lastImageId("sha256:def456")
            .imageSize(300_000_000L)
            .installDate("2025-06-01T12:00:00Z")
            .build();

    String json = OBJECT_MAPPER.writeValueAsString(original);
    FlowerSuperexecContainerConfig deserialized =
        OBJECT_MAPPER.readValue(json, FlowerSuperexecContainerConfig.class);

    assertEquals(original, deserialized);
  }

  @Test
  void flowerConfigsAreNotUpdatableContainers() {
    FlowerSupernodeContainerConfig supernode =
        FlowerSupernodeContainerConfig.builder().name("sn").image("flwr/supernode:1.26.1").build();

    FlowerSuperexecContainerConfig superexec =
        FlowerSuperexecContainerConfig.builder()
            .name("se")
            .image("timmyjc/superexec-test:0.0.1")
            .build();

    assertFalse(supernode instanceof UpdatableContainer);
    assertFalse(superexec instanceof UpdatableContainer);
  }

  @Test
  void flowerConfigsImplementFlowerContainer() {
    FlowerSupernodeContainerConfig supernode =
        FlowerSupernodeContainerConfig.builder().name("sn").image("flwr/supernode:1.26.1").build();

    FlowerSuperexecContainerConfig superexec =
        FlowerSuperexecContainerConfig.builder()
            .name("se")
            .image("timmyjc/superexec-test:0.0.1")
            .build();

    assertInstanceOf(FlowerContainer.class, supernode);
    assertInstanceOf(FlowerContainer.class, superexec);
  }
}
