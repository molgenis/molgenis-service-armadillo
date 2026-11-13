package org.molgenis.armadillo.container;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.molgenis.armadillo.metadata.ContainersMetadata;

class DatashieldContainerConfigSerializationTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void datashieldSpecificFieldsAreWrittenWhenMetadataIsSerialized() throws Exception {
    DatashieldContainerConfig config =
        DatashieldContainerConfig.builder()
            .name("test")
            .host("localhost")
            .port(6313)
            .packageWhitelist(Set.of("dsBase", "testpackage"))
            .functionBlacklist(Set.of("forbidden"))
            .datashieldROptions(Map.of("datashield.seed", "123456789"))
            .build();

    ContainersMetadata metadata = ContainersMetadata.create();
    metadata.getContainers().put("test", config);

    String json = OBJECT_MAPPER.writeValueAsString(metadata);
    JsonNode containerNode = OBJECT_MAPPER.readTree(json).path("containers").path("test");

    assertTrue(containerNode.has("packageWhitelist"));
    assertTrue(containerNode.has("functionBlacklist"));
    assertTrue(containerNode.has("datashieldROptions"));

    Set<String> whitelist =
        StreamSupport.stream(containerNode.path("packageWhitelist").spliterator(), false)
            .map(JsonNode::asText)
            .collect(Collectors.toSet());
    assertEquals(Set.of("dsBase", "testpackage"), whitelist);

    Set<String> blacklist =
        StreamSupport.stream(containerNode.path("functionBlacklist").spliterator(), false)
            .map(JsonNode::asText)
            .collect(Collectors.toSet());
    assertEquals(Set.of("forbidden"), blacklist);

    Map<String, String> options =
        OBJECT_MAPPER.convertValue(
            containerNode.path("datashieldROptions"), new TypeReference<Map<String, String>>() {});
    assertEquals("123456789", options.get("datashield.seed"));
  }
}
