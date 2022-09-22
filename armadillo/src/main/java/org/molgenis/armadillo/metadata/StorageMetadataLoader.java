package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class StorageMetadataLoader implements MetadataLoader {

  private final ArmadilloStorageService storage;

  private static final Logger LOGGER = LoggerFactory.getLogger(StorageMetadataLoader.class);
  public static final String METADATA_FILE = "metadata.json";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public StorageMetadataLoader(ArmadilloStorageService storageService) {
    this.storage = storageService;
  }

  @Override
  public synchronized ArmadilloMetadata save(ArmadilloMetadata metadata) {
    try {
      String json = objectMapper.writeValueAsString(metadata);
      try (InputStream inputStream = new ByteArrayInputStream(json.getBytes())) {
        storage.saveSystemFile(inputStream, METADATA_FILE, MediaType.APPLICATION_JSON);
      }
    } catch (Exception e) {
      throw new StorageException(e);
    }
    return load();
  }

  public ArmadilloMetadata load() {
    String result;
    try (InputStream inputStream = storage.loadSystemFile(StorageMetadataLoader.METADATA_FILE)) {
      result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      ArmadilloMetadata temp = objectMapper.readValue(result, ArmadilloMetadata.class);
      return Objects.requireNonNullElseGet(temp, ArmadilloMetadata::create);
    } catch (ValueInstantiationException e) {
      // this is serious, manually edited file maybe?
      LOGGER.error(
          String.format(
              "Parsing of %s failed: %s", StorageMetadataLoader.METADATA_FILE, e.getMessage()));
      System.exit(-1);
      return ArmadilloMetadata.create();
    } catch (Exception e) {
      // this probably just means first time
      return ArmadilloMetadata.create();
    }
  }
}
