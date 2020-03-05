package org.molgenis.datashield.service;

import java.io.InputStream;
import org.springframework.http.MediaType;

/** Reads and writes data to storage. */
public interface StorageService {
  void save(InputStream is, String objectName, MediaType mediaType);

  InputStream load(String objectName);
}
