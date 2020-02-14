package org.molgenis.datashield.service;

import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;

import io.swagger.client.model.Attribute;
import io.swagger.client.model.AttributeData;
import io.swagger.client.model.EntityType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.tomcat.util.http.fileupload.util.Streams;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

public class DownloadServiceImpl {

  private RestTemplate restTemplate;
  private static final String DOWNLOAD_URL = "/menu/main/dataexplorer/download";
  private static final String METADATA_URL = "/api/metadata/{entityTypeId}?flattenAttributes=true";

  public DownloadServiceImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public EntityType getMetadata(String entityTypeId) {
    return restTemplate.getForObject(METADATA_URL, EntityType.class, entityTypeId);
  }

  /**
   * Downloads CSV for entity type.
   * @param entityTypeId the ID of the entity type.
   * @return ResponseEntity to stream the CSV from
   */
  public ResponseEntity<Resource> download(String entityTypeId, List<String> attributeNames) {
    Map<String, Object> request = Map.of("entityTypeId", entityTypeId,
        "query", Map.of("rules", List.of(List.of())),
        "attributeNames", attributeNames,
        "colNames", "ATTRIBUTE_NAMES",
        "entityValues", "ENTITY_IDS",
        "downloadType", "DOWNLOAD_TYPE_CSV");

    LinkedMultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add("dataRequest", request);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MULTIPART_FORM_DATA);
    HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(params, headers);
    return restTemplate.postForEntity(DOWNLOAD_URL, requestEntity, Resource.class);
  }

  public static void main(String... args) throws IOException {
    String token = "test";
    String server = "https://latest.test.molgenis.org";
    String entityTypeId = "aaaac4buj5fdzlvl6uxmg6iaae";
    RestTemplate restTemplate =
        new RestTemplateBuilder()
            .messageConverters(
                List.of(new GsonHttpMessageConverter(),
                  new AllEncompassingFormHttpMessageConverter(),
                  new ResourceHttpMessageConverter(true)))
            .defaultHeader("X-Molgenis-Token", token)
            .rootUri(server)
            .build();
    DownloadServiceImpl downloadService = new DownloadServiceImpl(restTemplate);
    EntityType metadata = downloadService.getMetadata(entityTypeId);
    List<String> attributeNames = metadata.getData().getAttributes().getItems().stream()
        .map(Attribute::getData).map(AttributeData::getName).collect(toList());
    ResponseEntity<Resource> responseEntity = downloadService.download("aaaac4buj5fdzlvl6uxmg6iaae", attributeNames);
    InputStream inputStream = responseEntity.getBody().getInputStream();
    Logger.getAnonymousLogger().info(Streams.asString(inputStream));
  }
}
