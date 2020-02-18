package org.molgenis.datashield.service;

import org.molgenis.datashield.service.model.Table;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

public interface DownloadService
{
	Table getMetadata(String entityTypeId);

	ResponseEntity<Resource> download(Table table);
}
