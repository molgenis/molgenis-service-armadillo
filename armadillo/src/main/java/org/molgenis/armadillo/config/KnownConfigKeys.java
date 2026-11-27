package org.molgenis.armadillo.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

/** The configuration keys published in the Spring metadata files on the classpath. */
class KnownConfigKeys {

  private static final String METADATA_FILE = "META-INF/spring-configuration-metadata.json";

  private static final List<String> MAP_TYPES = List.of("java.util.Map", "java.util.Properties");

  private static final List<String> LIST_TYPES =
      List.of("java.util.List", "java.util.Set", "java.util.Collection");

  private enum ChildKeys {
    NONE,
    ANY_NAME,
    INDEXED
  }

  private record KnownKey(ConfigurationPropertyName name, ChildKeys childKeys) {}

  private final List<KnownKey> keys;

  private KnownConfigKeys(List<KnownKey> keys) {
    this.keys = keys;
  }

  static KnownConfigKeys loadFromClasspath() {
    ClassLoader loader = KnownConfigKeys.class.getClassLoader();
    ObjectMapper mapper = new ObjectMapper();
    List<KnownKey> keys = new ArrayList<>();
    try {
      Enumeration<URL> urls = loader.getResources(METADATA_FILE);
      while (urls.hasMoreElements()) {
        keys.addAll(readKeys(mapper, urls.nextElement()));
      }
    } catch (IOException e) {
      throw new UncheckedIOException("Could not read configuration metadata", e);
    }
    return new KnownConfigKeys(keys);
  }

  boolean contains(String key) {
    ConfigurationPropertyName name = ConfigurationPropertyName.adapt(key, '.');
    return keys.stream().anyMatch(known -> accepts(known, name));
  }

  private static boolean accepts(KnownKey known, ConfigurationPropertyName name) {
    if (known.name().equals(name)) {
      return true;
    }
    if (!known.name().isAncestorOf(name)) {
      return false;
    }
    return switch (known.childKeys()) {
      case ANY_NAME -> true;
      case INDEXED -> name.chop(known.name().getNumberOfElements() + 1).isLastElementIndexed();
      case NONE -> false;
    };
  }

  private static List<KnownKey> readKeys(ObjectMapper mapper, URL url) throws IOException {
    List<KnownKey> keys = new ArrayList<>();
    try (InputStream stream = url.openStream()) {
      for (JsonNode property : mapper.readTree(stream).path("properties")) {
        if (!isRemoved(property)) {
          keys.add(toKnownKey(property));
        }
      }
    }
    return keys;
  }

  private static boolean isRemoved(JsonNode property) {
    return property.path("deprecation").path("level").asText().equals("error");
  }

  private static KnownKey toKnownKey(JsonNode property) {
    return new KnownKey(
        ConfigurationPropertyName.adapt(property.path("name").asText(), '.'),
        findChildKeys(property.path("type").asText("")));
  }

  private static ChildKeys findChildKeys(String type) {
    if (MAP_TYPES.stream().anyMatch(type::startsWith)) {
      return ChildKeys.ANY_NAME;
    }
    if (type.endsWith("[]") || LIST_TYPES.stream().anyMatch(type::startsWith)) {
      return ChildKeys.INDEXED;
    }
    return ChildKeys.NONE;
  }
}
