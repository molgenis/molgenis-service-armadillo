package org.molgenis.armadillo.metadata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.molgenis.armadillo.exceptions.StorageException;
import org.molgenis.armadillo.storage.ArmadilloStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

public abstract class StorageJsonLoader<T extends Persistable> {

  @Autowired // constructor injection not possible because of bean inheritance
  private ArmadilloStorageService storage;

  private static final Logger LOGGER = LoggerFactory.getLogger(StorageJsonLoader.class);
  private static final ObjectMapper objectMapper = new ObjectMapper();

  public synchronized T save(T metadata) {
    try {
      String json = objectMapper.writeValueAsString(metadata);
      try (InputStream inputStream = new ByteArrayInputStream(json.getBytes())) {
        storage.saveSystemFile(inputStream, getJsonFilename(), MediaType.APPLICATION_JSON);
      }
    } catch (Exception e) {
      throw new StorageException(e);
    }
    return load();
  }

  public T load() {
    String result;
    try (InputStream inputStream = storage.loadSystemFile(getJsonFilename())) {
      result = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
      var temp = objectMapper.readValue(result, getTargetClass());

      //noinspection unchecked
      return temp == null ? createDefault() : (T) temp;

    } catch (ValueInstantiationException e) {
      // this is serious, manually edited file maybe?
      LOGGER.error(String.format("Parsing of %s failed: %s", getJsonFilename(), e.getMessage()));
      System.exit(-1);
      return createDefault();
    } catch (Exception e) {
      // this probably just means first time
      return createDefault();
    }
  }

  public abstract T createDefault();

  public abstract Class<? extends Persistable> getTargetClass();

  public abstract String getJsonFilename();
}
