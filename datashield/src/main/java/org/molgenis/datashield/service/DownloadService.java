package org.molgenis.datashield.service;

import org.molgenis.r.model.Table;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface DownloadService {
  Table getMetadata(String entityTypeId);

  boolean metadataExists(String entityTyped);

  ResponseEntity<Resource> download(Table table);
}
