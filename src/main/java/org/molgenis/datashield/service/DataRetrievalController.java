package org.molgenis.datashield.service;

import static java.util.stream.Collectors.toList;

import io.swagger.client.model.Attribute;
import io.swagger.client.model.AttributeData;
import io.swagger.client.model.EntityType;
import java.util.List;
import java.util.stream.Collectors;
import org.molgenis.datashield.service.model.Column;
import org.molgenis.datashield.service.model.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DataRetrievalController {
  @Autowired
  DownloadServiceImpl downloadService;

  @GetMapping("/retrieve/{entityTypeId}")
  public Resource retrieve(@PathVariable String entityTypeId) {
    Table table = downloadService.getMetadata(entityTypeId);
    ResponseEntity<Resource> response = downloadService.download(table);
    return response.getBody();
  }
}
