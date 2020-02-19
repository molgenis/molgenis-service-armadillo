package org.molgenis.datashield.service;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import java.util.List;
import java.util.Map;
import org.molgenis.api.metadata.model.Attribute;
import org.molgenis.api.metadata.model.AttributeData;
import org.molgenis.api.metadata.model.AttributeData.TypeEnum;
import org.molgenis.api.metadata.model.EntityType;
import org.molgenis.datashield.service.model.Column;
import org.molgenis.datashield.service.model.ColumnType;
import org.molgenis.datashield.service.model.Table;
import org.molgenis.datashield.service.model.Table.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
public class DownloadServiceImpl {
  private static final String DOWNLOAD_URL = "/menu/main/dataexplorer/download";
  private static final String METADATA_URL = "/api/metadata/{entityTypeId}?flattenAttributes=true";

  @Autowired private RestTemplate restTemplate;

  /**
   * Retrieves metadata for entity type and maps to datashield {@link Table}.
   *
   * @param entityTypeId the ID of the entity type, dots will be replaced with underscores
   * @return the {@link Table}
   */
  public Table getMetadata(String entityTypeId) {
    EntityType entityType =
        restTemplate.getForObject(METADATA_URL, EntityType.class, normalize(entityTypeId));
    Builder tableBuilder = Table.builder().setName(entityTypeId);
    entityType.getData().getAttributes().getItems().stream()
        .map(Attribute::getData)
        .filter(attributeData -> attributeData.getType() != TypeEnum.COMPOUND)
        .map(this::toColumn)
        .forEach(tableBuilder::addColumn);
    return tableBuilder.build();
  }

  public static String normalize(String entityTypeId) {
    return entityTypeId.replace('.', '_');
  }

  private Column toColumn(AttributeData attribute) {
    return Column.builder().setName(attribute.getName()).setType(toColumnType(attribute)).build();
  }

  private ColumnType toColumnType(AttributeData attributeData) {
    switch (attributeData.getType()) {
      case INT:
        return ColumnType.INT;
      case BOOL:
        return ColumnType.BOOL;
      case DATE:
        return ColumnType.DATE;
      case SCRIPT:
      case STRING:
      case TEXT:
      case HTML:
        return ColumnType.STRING;
      case XREF:
      case CATEGORICAL:
        String refEntityLink = attributeData.getRefEntityType().getSelf();
        EntityType refEntityType =
            restTemplate.getForObject(refEntityLink + "?flattenAttributes=true", EntityType.class);
        AttributeData refEntityIdAttribute =
            refEntityType.getData().getAttributes().getItems().stream()
                .map(Attribute::getData)
                .filter(AttributeData::isIdAttribute)
                .findFirst()
                .orElseThrow();
        return toColumnType(refEntityIdAttribute);
      default:
        throw new IllegalArgumentException(
            "Cannot convert type " + attributeData.getType().getValue());
    }
  }

  /**
   * Downloads CSV for entity type.
   *
   * @param table the Table to download
   * @return ResponseEntity to stream the CSV from
   */
  public ResponseEntity<Resource> download(Table table) {
    List<String> columnNames = table.columns().stream().map(Column::name).collect(toList());
    Map<String, Object> request =
        Map.of(
            "entityTypeId",
            normalize(table.name()),
            "query",
            Map.of("rules", List.of(List.of())),
            "attributeNames",
            columnNames,
            "colNames",
            "ATTRIBUTE_NAMES",
            "entityValues",
            "ENTITY_IDS",
            "downloadType",
            "DOWNLOAD_TYPE_CSV");
    LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add("dataRequest", request);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MULTIPART_FORM_DATA);
    HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity =
        new HttpEntity<>(params, headers);
    return restTemplate.postForEntity(DOWNLOAD_URL, requestEntity, Resource.class);
  }
}
